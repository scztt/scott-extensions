+ NodeProxy {

	// Try to get a value of an input parameter, either from the synth default or the nodemap
	getValueOf {
		| param |
		var nm, value;
		nm = this.nodeMap.settings[param];
		if( nm.notNil, {
			value = nm.value
		},{
			// for now, only do this for nodeproxys with 1 object, otherwise it gets complicated
			if( this.objects.size == 1, {
				value = this.objects[0].synthDef.allControlNames.detect({
					 |cn| cn.name == param.asSymbol
				});
				value = value.notNil.if({ value.defaultValue });
			})
		})
		^value
	}

}