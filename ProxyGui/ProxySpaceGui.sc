
ProxySpaceGui {
	var controller, <proxyspace, proxyPlayerGuiDict, nodesHash, parent;
	var <view;
	var lastProxyCount=0, minimized=false, refreshNeeded=false, origin;
	var cBackground, cLines;
	var <refreshJack;
	
	*new {
		| proxyspace, parent, controller |
		^super.new.init( proxyspace, parent, controller );
	}
	
	init {
		| px, pr, ct |
		proxyspace = px;
		parent = pr;
		controller = ct;
				
		proxyPlayerGuiDict = IdentityDictionary.new;
		
		view = VerticalStackedView( parent.view, origin, proxyspace.name );
		this.refresh;
	}
	
	refresh {
		var newGui, oldSize;

		if( (proxyspace.envir.hash == nodesHash).not, {   // hashes are different, so a change has occurred.
			nodesHash = proxyspace.envir.hash;
			oldSize = proxyPlayerGuiDict.size;
			proxyPlayerGuiDict.sync( proxyspace.envir,
				{ 	| key, proxy |
					switch (proxy.rate, 
						\audio, {
							AudioProxyGui( proxy, format("% [%]", key.asString, proxy.bus.numChannels ), this, controller ) },
						\control, {
							ControlProxyGui( proxy, format("%", key.asString ), this, controller ) } );
				},{	| key, view |
				});
			view.prRefreshPositions;		// Any time we hit this code, something is added/removed and we need a refresh
			(oldSize == proxyPlayerGuiDict.size).not.if({ parent.view.refresh });
		});
		this.refreshChildren;
	}

	refreshChildren {
		proxyPlayerGuiDict.keysValuesDo({ |k,v| v.refresh })
	}
}

+ IdentityDictionary {
	sync {
		| syncTarget, addFunc, removeFunc |
		var toRemove, toAdd, v;
		toRemove = List.new;
		
		// Additions
		syncTarget.keysValuesDo({
			| key, val |
			if( this.includesKey(key).not, { 
				this[key] = addFunc.value( key, val );
			})
		});
		
		// Subtractions
		this.keysValuesDo({
			| key, val |
			v = syncTarget[key];
			if( v.isNil, {
				toRemove.add( key );
				removeFunc.value( key, val );
			})
		});
		toRemove.do( this.removeAt(_) )
	}
}

