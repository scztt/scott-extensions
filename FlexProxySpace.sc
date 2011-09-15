FlexProxySpace : ProxySpace {

	*initClass { all = IdentityDictionary.new.know_(true) }

	makeProxy {
		^proxyClass.asClass.new
	}
	
	init { arg argServer, argName, argClock;
		server = argServer;
		clock = argClock;
		name = argName.asSymbol ? "";
		if(clock.notNil) { this.quant = 1.0 };
		if(argName.notNil) { this.add };
	}
}

+Object {
	proxy {
		var proxy, env;
		currentEnvironment.isKindOf(ProxySpace).if({
			proxy = NodeProxy.new(currentEnvironment.server);
			proxy.clock = currentEnvironment.clock;
			proxy.awake = currentEnvironment.awake;
			if(currentEnvironment.fadeTime.notNil) { proxy.fadeTime = currentEnvironment.fadeTime };
			if(currentEnvironment.group.isPlaying) { proxy.parentGroup = currentEnvironment.group };
			if(currentEnvironment.quant.notNil) { proxy.quant = currentEnvironment.quant };
			^proxy.source_(this);
		},{
			
		})
	}
	
	pr {
		this.proxy();
	}
}