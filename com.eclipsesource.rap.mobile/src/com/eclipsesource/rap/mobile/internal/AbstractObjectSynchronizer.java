/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.rap.mobile.internal;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rwt.RWT;
import org.eclipse.rwt.internal.lifecycle.LifeCycleUtil;
import org.eclipse.rwt.internal.protocol.ClientObjectFactory;
import org.eclipse.rwt.internal.protocol.IClientObject;
import org.eclipse.rwt.lifecycle.PhaseEvent;
import org.eclipse.rwt.lifecycle.PhaseId;
import org.eclipse.rwt.lifecycle.PhaseListener;
import org.eclipse.rwt.service.IServiceStore;
import org.eclipse.rwt.service.SessionStoreEvent;
import org.eclipse.rwt.service.SessionStoreListener;
import org.eclipse.swt.internal.widgets.WidgetAdapter;
import org.eclipse.swt.widgets.Display;


public abstract class AbstractObjectSynchronizer implements PhaseListener, SessionStoreListener {
  
  private final Display display;
  private final String id;
  private final Object object;
  private boolean initialized;

  public AbstractObjectSynchronizer( Object object ) {
    this.object = object;
    this.initialized = false;
    RWT.getLifeCycle().addPhaseListener( this );
    RWT.getSessionStore().addSessionStoreListener( this );
    display = Display.getCurrent();
    WidgetAdapter widgetAdapter = new WidgetAdapter();
    id = widgetAdapter.getId();
  }

  public void beforePhase( PhaseEvent event ) {
    // do nothing
  }

  public void afterPhase( PhaseEvent event ) {
    Display sessionDisplay = LifeCycleUtil.getSessionDisplay();
    if( display == sessionDisplay ) {
      processLifeCycleMethods( event );
    }
  }

  private void processLifeCycleMethods( PhaseEvent event ) {
    PhaseId currentPhase = event.getPhaseId();
    if( currentPhase == PhaseId.PREPARE_UI_ROOT && !initialized ) {
      renderInitializationInternal();
      initialized = true;
    } else if( currentPhase == PhaseId.READ_DATA ) {
      readData( object );
      preserveValues( object );
    } else if( currentPhase == PhaseId.PROCESS_ACTION ) {
      processAction( object );
    } else if( currentPhase == PhaseId.RENDER ) {
      renderChanges( object );
    }
  }

  private void renderInitializationInternal() {
    IClientObject clientObject = ClientObjectFactory.getForId( id );
    renderInitialization( clientObject, object );
  }

  protected abstract void renderInitialization( IClientObject clientObject, Object object );

  protected abstract void readData( Object object );

  // TODO: move to Util
  public String readPropertyValue( String name ) {
    // TODO: WidgetUtil.readPropertyValue with id should be public
    HttpServletRequest request = RWT.getRequest();
    StringBuilder key = new StringBuilder();
    key.append( id );
    key.append( "." );
    key.append( name );
    return request.getParameter( key.toString() );
  }

  protected abstract void preserveValues( Object object );

  // TODO move to Util
  protected void preserveProperty( String name, Object value ) {
    IServiceStore serviceStore = RWT.getServiceStore();
    serviceStore.setAttribute( id + "." + name, value );
  }
  
  // TODO move to Util
  protected void preserveProperty( String name, int value ) {
    preserveProperty( name, new Integer( value ) );
  }
  
  // TODO move to Util
  protected void preserveProperty( String name, boolean value ) {
    preserveProperty( name, Boolean.valueOf( value ) );
  }

  protected abstract void processAction( Object object );

  protected abstract void renderChanges( Object object );
  
  // TODO move to Util
  public void renderProperty( String name, Object newValue, Object defaultValue ) {
    IServiceStore serviceStore = RWT.getServiceStore();
    IClientObject clientObject = ClientObjectFactory.getForId( id );
    Object attribute = serviceStore.getAttribute( id + "." + name );
    if( attribute == null ) {
      clientObject.set( name, newValue );
    } else if( !attribute.equals( newValue ) ) {
      if( defaultValue != null && !defaultValue.equals( attribute ) ) {
        clientObject.set( name, newValue );
      }
    }
  }
  
  // TODO move to Util
  public void renderProperty( String name, int newValue, int defaultValue ) {
    renderProperty( name, new Integer( newValue ), new Integer( defaultValue ) );
  }
  
  // TODO move to Util
  public void renderProperty( String name, boolean newValue, boolean defaultValue ) {
    renderProperty( name, Boolean.valueOf( newValue ), Boolean.valueOf( defaultValue ) );
  }

  public PhaseId getPhaseId() {
    return PhaseId.ANY;
  }

  public void beforeDestroy( SessionStoreEvent event ) {
    IClientObject clientObject = ClientObjectFactory.getForId( id );
    destroy( clientObject );
    RWT.getLifeCycle().removePhaseListener( this );
    RWT.getSessionStore().removeSessionStoreListener( this );
  }

  protected abstract void destroy( IClientObject clientObject );
  
  protected String getId() {
    return id;
  }
}
