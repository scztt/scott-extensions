MultiProxyWindow {
	var controller, <window, <view, <cView, container, <proxySpaces, spacesHash=0, refreshNeeded=false;
	var yPos=0, margin=2, space=2;
	var refreshJack;
	
	*new {
		^super.new.init;
	}
	
	init {
		controller = MPWController.new;
	
		window = SCWindow( "p.*", Rect( 400,400, 200, 400 ), resizable:false);
		window.view.background_( Color.grey(0.1));
		
		// Main MPW container
		container = HorizontalStackedView( window, 0@0, "", false);
		
		// Contains all the proxies
		view = VerticalStackedView( container, 0@0, "", false );
		
		// Contains controls
		cView = VerticalStackedView( CollapsableView( container, 0@0, "", false ), 
			0@0, "", false );
		
		proxySpaces = IdentityDictionary.new;
		
		controller.window = this;
		controller.controlsView = cView;
		
		this.refreshSpaces;
		this.refreshChildren;
		this.doRefreshLoop;
		window.front;
	}
	
	refreshSpaces {
		var proxyspace, oldHash, oldSize;
		//spacesHash.postln; DualEnvir.all.hash.postln;
		if( (spacesHash==DualEnvir.all.hash).not, {
			oldSize = DualEnvir.all.size;
			DualEnvir.all.keysValuesDo({
				| key, val |
				proxySpaces.includesKey(key).not.if({
					proxySpaces.put( key, ProxySpaceGui(val, this, controller))
				});
			});
			spacesHash = DualEnvir.all.hash;
			(oldSize == DualEnvir.all.size).not.if({ window.refreshPositions });
		});
		view.refreshPositions;
	}
	
	refresh {
		//"mp refresh".postln;
		this.refreshSpaces;
	}
	
	refreshChildren {
		proxySpaces.keysValuesDo({
			| key, val |
			val.refresh;
		})
	}
	
	doRefreshLoop {
		| delta = 1 |
		window.onClose_({ refreshJack.stop });
		refreshJack = SkipJack({ this.refreshChildren; this.refresh; }, delta, {window.isClosed});
	}
}