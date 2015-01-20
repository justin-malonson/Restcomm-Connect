/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.servlet.restcomm.mgcp.mscontrol;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.util.HashSet;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateConnection;
import org.mobicents.servlet.restcomm.mgcp.GetMediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mscontrol.MediaSessionController;
import org.mobicents.servlet.restcomm.mscontrol.messages.CloseMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.CreateMediaSession;
import org.mobicents.servlet.restcomm.mscontrol.messages.Mute;
import org.mobicents.servlet.restcomm.mscontrol.messages.Unmute;
import org.mobicents.servlet.restcomm.patterns.Observe;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMediaSessionController extends MediaSessionController {

    // Logging
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    // FSM.
    private final FiniteStateMachine fsm;

    // FSM states
    private final State uninitialized;
    private final State active;
    private final State inactive;
    private final State failed;

    // Intermediate states
    private final State acquiringMediaGatewayInfo;
    private final State acquiringMediaSession;
    private final State acquiringBridge;
    private final State acquiringRemoteConnection;
    private final State initializingRemoteConnection;
    private final State openingRemoteConnection;
    private final State updatingRemoteConnection;
    private final State closingRemoteConnection;
    private final State acquiringInternalLink;
    private final State initializingInternalLink;
    private final State openingInternalLink;
    private final State updatingInternalLink;
    private final State closingInternalLink;
    private final State muting;
    private final State unmuting;
    private final State pending;

    // MGCP runtime stuff.
    private final ActorRef mediaGateway;
    private MediaGatewayInfo gatewayInfo;
    private MediaSession session;
    private ActorRef bridge;
    private ActorRef remoteConn;
    private ActorRef internalLink;
    private ActorRef internalLinkEndpoint;
    private ConnectionMode internalLinkMode;

    public MgcpMediaSessionController(final ActorRef mediaGateway) {
        super();
        final ActorRef source = self();

        // Initialize the states for the FSM.
        this.uninitialized = new State("uninitialized", null, null);
        this.active = new State("active", null, null);
        this.inactive = new State("inactive", null, null);
        this.failed = new State("failed", null, null);

        // Intermediate states
        this.acquiringMediaGatewayInfo = new State("acquiring media gateway info", null, null);
        this.acquiringMediaSession = new State("acquiring media session", null, null);
        this.acquiringBridge = new State("acquiring media bridge", null, null);
        this.acquiringRemoteConnection = new State("acquiring remote connection", null, null);
        this.initializingRemoteConnection = new State("initializing remote connection", null, null);
        this.openingRemoteConnection = new State("opening remote connection", null, null);
        this.updatingRemoteConnection = new State("updating remote connection", null, null);
        this.closingRemoteConnection = new State("closing remote connection", null, null);
        this.acquiringInternalLink = new State("acquiring internal link", null, null);
        this.initializingInternalLink = new State("initializing internal link", null, null);
        this.openingInternalLink = new State("opening internal link", null, null);
        this.updatingInternalLink = new State("updating internal link", null, null);
        this.closingInternalLink = new State("closing internal link", null, null);
        this.muting = new State("muting", null, null);
        this.unmuting = new State("unmuting", null, null);
        this.pending = new State("pending", null, null);

        // Transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(this.uninitialized, this.acquiringMediaGatewayInfo));
        transitions.add(new Transition(this.acquiringMediaGatewayInfo, this.acquiringMediaSession));
        transitions.add(new Transition(this.acquiringMediaSession, this.acquiringBridge));
        transitions.add(new Transition(this.acquiringBridge, this.acquiringMediaSession));
        transitions.add(new Transition(this.acquiringMediaSession, this.acquiringRemoteConnection));
        transitions.add(new Transition(this.acquiringRemoteConnection, this.initializingRemoteConnection));
        transitions.add(new Transition(this.initializingRemoteConnection, this.openingRemoteConnection));
        transitions.add(new Transition(this.openingRemoteConnection, this.active));
        transitions.add(new Transition(this.openingRemoteConnection, this.failed));
        transitions.add(new Transition(this.openingRemoteConnection, this.pending));
        transitions.add(new Transition(this.active, this.muting));
        transitions.add(new Transition(this.active, this.unmuting));
        transitions.add(new Transition(this.active, this.updatingRemoteConnection));
        transitions.add(new Transition(this.active, this.closingRemoteConnection));
        transitions.add(new Transition(this.active, this.acquiringInternalLink));
        transitions.add(new Transition(this.active, this.closingInternalLink));
        transitions.add(new Transition(this.muting, this.active));
        transitions.add(new Transition(this.muting, this.closingRemoteConnection));
        transitions.add(new Transition(this.unmuting, this.active));
        transitions.add(new Transition(this.unmuting, this.closingRemoteConnection));
        transitions.add(new Transition(this.updatingRemoteConnection, this.active));
        transitions.add(new Transition(this.updatingRemoteConnection, this.closingRemoteConnection));
        transitions.add(new Transition(this.closingRemoteConnection, this.inactive));
        transitions.add(new Transition(this.closingRemoteConnection, this.closingInternalLink));
        transitions.add(new Transition(this.acquiringInternalLink, this.closingRemoteConnection));
        transitions.add(new Transition(this.acquiringInternalLink, this.initializingInternalLink));
        transitions.add(new Transition(this.initializingInternalLink, this.closingRemoteConnection));
        transitions.add(new Transition(this.initializingInternalLink, this.openingInternalLink));
        transitions.add(new Transition(this.openingInternalLink, this.closingRemoteConnection));
        transitions.add(new Transition(this.openingInternalLink, this.updatingInternalLink));
        transitions.add(new Transition(this.updatingInternalLink, this.closingRemoteConnection));
        transitions.add(new Transition(this.updatingInternalLink, this.closingInternalLink));
        transitions.add(new Transition(this.closingInternalLink, this.closingRemoteConnection));
        transitions.add(new Transition(this.closingInternalLink, this.inactive));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);

        // MGCP runtime stuff
        this.mediaGateway = mediaGateway;
    }

    /**
     * Checks whether the actor is currently in a certain state.
     * 
     * @param state The state to be checked
     * @return Returns true if the actor is currently in the state. Returns false otherwise.
     */
    private boolean is(State state) {
        return this.fsm.state().equals(state);
    }

    /*
     * EVENTS
     */
    @Override
    public void onReceive(Object message) throws Exception {
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();

        logger.info("********** MSController's Current State: \"" + state.toString());
        logger.info("********** MSController Processing Message: \"" + klass.getName() + " sender : " + sender.getClass());

        if (CreateMediaSession.class.equals(klass)) {
            onCreateMediaSession((CreateMediaSession) message, self, sender);
        } else if (MediaGatewayResponse.class.equals(klass)) {
            onMediaGatewayResponse((MediaGatewayResponse<?>) message, self, sender);
        } else if (ConnectionStateChanged.class.equals(klass)) {
            onConnectionStateChanged((ConnectionStateChanged) message, self, sender);
        } else if (LinkStateChanged.class.equals(klass)) {
            onLinkStateChanged((LinkStateChanged) message, self, sender);
        } else if (Mute.class.equals(klass)) {
            onMute((Mute)message, self, sender);
        } else if (Unmute.class.equals(klass)) {
            onUnmute((Unmute)message, self, sender);
        } else if (CloseMediaSession.class.equals(klass)) {
            onCloseMediaSession((CloseMediaSession) message, self, sender);
        }

    }

    private void onCreateMediaSession(CreateMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, acquiringMediaGatewayInfo);
    }
    
    private void onCloseMediaSession(CloseMediaSession message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, closingRemoteConnection);
    }

    private void onMediaGatewayResponse(MediaGatewayResponse<?> message, ActorRef self, ActorRef sender) throws Exception {
        if (is(acquiringMediaGatewayInfo)) {
            fsm.transition(message, acquiringMediaSession);
        } else if (is(acquiringMediaSession)) {
            fsm.transition(message, acquiringBridge);
        } else if (is(acquiringBridge)) {
            fsm.transition(message, acquiringRemoteConnection);
        } else if (is(acquiringRemoteConnection)) {
            fsm.transition(message, initializingRemoteConnection);
        } else if (is(acquiringInternalLink)) {
            fsm.transition(message, initializingInternalLink);
        }
    }

    private void onConnectionStateChanged(ConnectionStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        switch (message.state()) {
            case CLOSED:
                if (is(initializingRemoteConnection)) {
                    fsm.transition(message, openingRemoteConnection);
                } else if (is(openingRemoteConnection)) {
                    fsm.transition(message, failed);
                } else if (is(muting) || is(unmuting)) {
                    fsm.transition(message, closingRemoteConnection);
                } else if (is(closingRemoteConnection)) {
                    remoteConn = null;
                    if (this.internalLink != null) {
                        fsm.transition(message, closingInternalLink);
                    } else {
                        fsm.transition(message, inactive);
                    }
                }
                break;

            case HALF_OPEN:
                fsm.transition(message, pending);
                break;

            case OPEN:
                fsm.transition(message, active);
                break;

            default:
                break;
        }
    }

    private void onLinkStateChanged(LinkStateChanged message, ActorRef self, ActorRef sender) throws Exception {
        switch (message.state()) {
            case CLOSED:
                if (is(initializingInternalLink)) {
                    fsm.transition(message, openingInternalLink);
                } else if (is(openingInternalLink)) {
                    fsm.transition(message, closingRemoteConnection);
                } else if (is(closingInternalLink)) {
                    if (remoteConn != null) {
                        fsm.transition(message, active);
                    } else {
                        fsm.transition(message, inactive);
                    }
                }
                break;

            case OPEN:
                if (is(openingInternalLink)) {
                    fsm.transition(message, updatingInternalLink);
                } else if (is(updatingInternalLink)) {
                    fsm.transition(message, active);
                }
                break;

            default:
                break;
        }
    }
    
    private void onMute(Mute message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, muting);
    }

    private void onUnmute(Unmute message, ActorRef self, ActorRef sender) throws Exception {
        fsm.transition(message, unmuting);
    }

    /*
     * ACTIONS
     */
    private abstract class AbstractAction implements Action {

        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class AcquiringMediaGatewayInfo extends AbstractAction {

        public AcquiringMediaGatewayInfo(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            mediaGateway.tell(new GetMediaGatewayInfo(), self());
        }
    }

    private final class AcquiringMediaSession extends AbstractAction {

        public AcquiringMediaSession(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<MediaGatewayInfo> response = (MediaGatewayResponse<MediaGatewayInfo>) message;
            gatewayInfo = response.get();
            mediaGateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateMediaSession(), source);
        }
    }

    public final class AcquiringBridge extends AbstractAction {

        public AcquiringBridge(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<MediaSession> response = (MediaGatewayResponse<MediaSession>) message;
            session = response.get();
            mediaGateway.tell(new CreateBridgeEndpoint(session), source);
        }
    }

    private final class AcquiringRemoteConnection extends AbstractAction {

        public AcquiringRemoteConnection(final ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            bridge = response.get();
            mediaGateway.tell(new CreateConnection(session), source);
        }
    }

    private final class InitializingRemoteConnection extends AbstractAction {

        public InitializingRemoteConnection(ActorRef source) {
            super(source);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void execute(final Object message) throws Exception {
            final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>) message;
            remoteConn = response.get();
            remoteConn.tell(new Observe(source), source);
            remoteConn.tell(new InitializeConnection(bridge), source);
        }
    }

}
