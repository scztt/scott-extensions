// A storable version of a NodeMap. An envir is needed to reconstitute:
//    StoredNodeMap + Envir = NodeMap
StoredProxyNodeMap {
	var <>settings, <nodeMap, str;
	
	*loadFromFile { 
		| path |
		var f, str, settings;
		settings = IdentityDictionary.readTextArchive( path );
		^super.new.initWithSettings( settings )
	}
	
	*new {
		| nodeMap, envir |
		^super.new.init( nodeMap, envir );
	}
	
	initWithSettings {
		| s |
		settings = s
	}
	
	init {
		| nodeMap, envir |
		var eKey;
		settings = IdentityDictionary.new;
		nodeMap.settings.keysValuesDo({
			| key, value |
			if( value.isMapped, {
				// Find key in the current envir...
				settings[key] = StoredProxyNodeMapSetting.fromProxyNodeMapSetting( value, envir );
			},{
				settings[key] = value.value ;
			})
		})
	}	
	
	saveToFile {
		| path |
		var f;
		settings.writeTextArchive( path );
	}
	
	createNodeMap {
		| envir |
		var newSettings, nm;
		nm = ProxyNodeMap.new;
		settings.keysValuesDo({
			| key, value |
			if( value.class == StoredProxyNodeMapSetting, {
				nm.settings[key] = value.createProxyNodeMapSetting( envir );
			},{
				nm.settings[key] = ProxyNodeMapSetting( key, value );
			})
		});
		nm.updateBundle;
		^nm
	}
}

StoredProxyNodeMapSetting {
	var key, proxyValue, busNumChannels, isMultiChannel, isMapped, rate;
	
	*fromProxyNodeMapSetting {
		| pnms, envir |
		^super.new.init( pnms, envir ) 
	}
	
	init {
		| pnms, envir |
		var val;
		if( pnms.isMapped, {
			val = envir.findPathToValue( pnms.value );
			if( val.isNil, {
				val = DualEnvir.findPathToValue( pnms.value );
			});
			proxyValue = val;
		},{
			proxyValue = pnms.value;
		});
		key = pnms.key; 
		
		busNumChannels = pnms.busNumChannels;
		isMultiChannel = pnms.isMultiChannel;
		isMapped = pnms.isMapped;
		rate = pnms.rate;
	}
	
	createProxyNodeMapSetting {
		var pnms;
		var value;
		value = try { proxyValue.interpret } 
			{ ("unable to find: " ++ proxyValue).postln; nil};
		pnms = ProxyNodeMapSetting( key, value, busNumChannels );
			pnms.isMultiChannel = isMultiChannel;
			pnms.isMapped = isMapped;
			pnms.rate = rate;
		^pnms
	}
}

+ DualEnvir {
	*findPathToValue {
		| value |
		var key;
		this.all.keysValuesDo({
			| k, v |
			key = v.findPathToValue( value );
			if( key.postln.notNil, { ^key })
		});
		^key
	}
	
	findPathToValue {
		| value |
		var key;
		key = this.envir.findKeyForValue( value );
		^ if( key.notNil, 
			{format( "DualEnvir.all[%][%]", "\\"++this.name.asString,"\\"++key.asString) },
			{nil} )
	}

}

+ DualEnvir {
	makeStoredNodeMapDictionary {
		| ... keys |
		var nodeMaps;
		keys.postln;
		nodeMaps = IdentityDictionary.new;
		if( keys.isEmpty, {
			envir.keysValuesDo({
				|key, value|
				nodeMaps.put( key, value.nodeMap.storable( this ) );
			})
		},{
			keys.do({
				| key |
				if( this.envir.includesKey( key ), {
					nodeMaps.put( key, this.envir[key].nodeMap.storable( this ) );
				})
			})
		});
		^nodeMaps
	}
	
	applyNodeMaps {
	}
}

+ ProxyNodeMap {
	storable {
		| envir |
		^StoredProxyNodeMap( this, envir )
	}
}


+ NodeMapSetting {
	updateValueFromEnvir {
		| envir |
		
	}
}