MPWController {
	var <>window, <>scope, <>controlsView, <controlViews;
	
	*new {
		^super.new.init
	}
	
	init {

	}
	
	setScopeBus {
		| index, rate, channels |
	}

	createControlViewFor {
		| proxy, proxyname |
		switch( proxy.rate,
		\control, {
			"add control editor".postln;
			//controlViews.add( ControlProxyEditor( proxy, controlsView, this ) );
		},
		\audio, {
			"add audio editor".postln;
			controlViews.add( AudioProxyEditor( proxy, proxyname, controlsView, this ) );
		})
	}
	
	removeControlView {
		| cv |
		
	}
}