/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.queue;

import org.apache.activemq.flow.Flow;
import org.apache.activemq.flow.FlowController;
import org.apache.activemq.flow.IFlowResource;
import org.apache.activemq.flow.IFlowSizeLimiter;
import org.apache.activemq.flow.ISinkController;
import org.apache.activemq.flow.ISourceController;
import org.apache.activemq.flow.SizeLimiter;
import org.apache.activemq.protobuf.AsciiBuffer;
import org.apache.activemq.queue.CursoredQueue.Cursor;
import org.apache.activemq.queue.CursoredQueue.QueueElement;
import org.apache.activemq.queue.QueueStore.PersistentQueue;
import org.apache.activemq.queue.QueueStore.QueueDescriptor;
import org.apache.activemq.util.Mapper;

public class ExclusivePersistentQueue<K, E> extends AbstractFlowQueue<E> implements PersistentQueue<K, E> {
    private CursoredQueue<E> queue;
    private final FlowController<E> controller;
    private IFlowSizeLimiter<E> limiter;
    private boolean started = true;
    private Cursor<E> cursor;
    private final QueueDescriptor queueDescriptor;
    private PersistencePolicy<E> persistencePolicy;
    private QueueStore<K, E> queueStore;
    private Mapper<Long, E> expirationMapper;
    private boolean initialized;

    /**
     * Creates a flow queue that can handle multiple flows.
     * 
     * @param flow
     *            The {@link Flow}
     * @param name
     *            The name of the queue.
     * @param limiter
     *            The size limiter for the queue.
     */
    public ExclusivePersistentQueue(String name, IFlowSizeLimiter<E> limiter) {
        super(name);
        this.queueDescriptor = new QueueStore.QueueDescriptor();
        queueDescriptor.setQueueName(new AsciiBuffer(super.getResourceName()));
        queueDescriptor.setQueueType(QueueDescriptor.EXCLUSIVE);

        //TODO flow should be serialized as part of the subscription. 
        this.controller = new FlowController<E>(null, new Flow(name, false), limiter, this);
        super.onFlowOpened(controller);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.activemq.queue.QueueStore.PersistentQueue#initialize(long,
     * long, int, long)
     */
    public synchronized void initialize(long sequenceMin, long sequenceMax, int count, long size) {
        if (initialized) {
            throw new IllegalStateException("Queue already initialized");
        }

        //Initialize the limiter:
        if (count > 0) {
            limiter.add(count, size);
        }

        queue = new CursoredQueue<E>(persistencePolicy, expirationMapper, controller.getFlow(), queueDescriptor, queueStore, this) {

            @Override
            protected void acknowledge(QueueElement<E> qe) {
                synchronized (ExclusivePersistentQueue.this) {
                    E elem = qe.getElement();
                    if (qe.delete()) {
                        if (!qe.acquired) {
                            controller.elementDispatched(elem);
                        }
                    }
                }
            }

            @Override
            protected int getElementSize(E elem) {
                return limiter.getElementSize(elem);
            }

            @Override
            protected void requestDispatch() {
                notifyReady();
            }
        };

        queue.initialize(sequenceMin, sequenceMax, count, size);
        initialized = true;
    }
    
    public void connect(Subscription<E> sub)
    {
        //Open a cursor for the queue:
        FlowController<QueueElement<E>> memoryController = null;
        if (persistencePolicy.isPagingEnabled()) {
            IFlowSizeLimiter<QueueElement<E>> limiter = new SizeLimiter<QueueElement<E>>(persistencePolicy.getPagingInMemorySize(), persistencePolicy.getPagingInMemorySize() / 2) {
                public int getElementSize(QueueElement<E> qe) {
                    return qe.size;
                };
            };

            memoryController = new FlowController<QueueElement<E>>(null, controller.getFlow(), limiter, this) {
                @Override
                public IFlowResource getFlowResource() {
                    return ExclusivePersistentQueue.this;
                }
            };
            controller.useOverFlowQueue(false);
            controller.setExecutor(dispatcher.createPriorityExecutor(dispatcher.getDispatchPriorities() - 1));
        }

        cursor = queue.openCursor(getResourceName(), memoryController, true, true);
    }
    
    

    protected final ISinkController<E> getSinkController(E elem, ISourceController<?> source) {
        return controller;
    }

    public void add(E elem, ISourceController<?> source) {
        synchronized (this) {
            assert initialized;
            controller.add(elem, source);
            accepted(source, elem);
        }
    }

    public boolean offer(E elem, ISourceController<?> source) {
        synchronized (this) {
            assert initialized;
            if (controller.offer(elem, source)) {
                accepted(source, elem);
                return true;
            }
            return false;
        }
    }

    /**
     * Called when the controller accepts a message for this queue.
     */
    public synchronized void flowElemAccepted(ISourceController<E> controller, E elem) {
        accepted(null, elem);
    }

    private final void accepted(ISourceController<?> source, E elem) {
        queue.add(source, elem);
        if (started) {
            notifyReady();
        }
    }

    public synchronized void start() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
        if (!started) {
            started = true;
            if (isDispatchReady()) {
                notifyReady();
            }
            queue.start();
        }
    }

    public synchronized void stop() {
        if (started) {
            started = false;
            queue.stop();
        }
    }

    public FlowController<E> getFlowController(Flow flow) {
        return controller;
    }

    public final boolean isDispatchReady() {
        return started && !cursor.isReady();
    }

    public final boolean pollingDispatch() {
        E elem = poll();

        if (elem != null) {
            drain.drain(elem, controller);
            return true;
        } else {
            return false;
        }
    }

    public final E poll() {
        synchronized (this) {
            if (!started) {
                return null;
            }

            QueueElement<E> qe = cursor.getNext();

            // FIXME the release should really be done after dispatch.
            // doing it here saves us from having to resynchronize
            // after dispatch, but release limiter space too soon.
            if (qe != null) {
                if (autoRelease) {
                    controller.elementDispatched(qe.getElement());
                }
                return qe.getElement();
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return "SingleFlowQueue:" + getResourceName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.activemq.queue.QueueStore.PersistentQueue#getDescriptor()
     */
    public QueueDescriptor getDescriptor() {
        return queueDescriptor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.activemq.queue.QueueStore.PersistentQueue#setPersistencePolicy
     * (org.apache.activemq.queue.PersistencePolicy)
     */
    public void setPersistencePolicy(PersistencePolicy<E> persistencePolicy) {
        this.persistencePolicy = persistencePolicy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.activemq.queue.QueueStore.PersistentQueue#setStore(org.apache
     * .activemq.queue.QueueStore)
     */
    public void setStore(QueueStore<K, E> store) {
        this.queueStore = store;
    }

    /**
     * @param expirationMapper
     */
    public void setExpirationMapper(Mapper<Long, E> expirationMapper) {
        this.expirationMapper = expirationMapper;

    }

    /**
     * @return The size of the elements in this queue or -1 if not yet known.
     */
    public synchronized long getEnqueuedSize() {
        if(!initialized)
        {
            return -1;
        }
        return limiter.getSize();
    }

    /**
     * @return The count of the elements in this queue or -1 if not yet known.
     */
    public synchronized long getEnqueuedCount() {
        if(!initialized)
        {
            return -1;
        }
        return queue.getEnqueuedCount();
    }
}