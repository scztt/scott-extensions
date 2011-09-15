// Signals+slots

ScalarModel {
	var <value, <spec, <valueSignal, <specSignal;
	
	*new {
		^super.new.init
	}
	
	init {
		value = 0;
		spec = ControlSpec();
		valueSignal = Model();
		specSignal = Model();
	}
	
	spec_{
		| inSpec |
		if( inSpec != spec, {
			spec = inSpec;
			specSignal.changed(spec);
		})
	}
	
	value_{
		| inValue |
		if( value != inValue, {
			value = spec.constrain( inValue );
			valueSignal.changed(value);
		})
	}
	
	mapValue_{
		| inValue |
		if( value != spec.unmap(inValue), {
			value = spec.map( inValue );
			valueSignal.changed(value);
		})
	}
	
	connectTo {
		| object |
		object.isKindOf( Slider )
	}
	
	connectToSlider {
		| inSlider |
		var inFunc, outFunc;

		inFunc = {
			| signal, value, spec |
			{ inSlider.value = spec.unmap(value) }.defer;
		};

		outFunc = {
			| view |
			this.value = view.value;
		};
	}
}

BusModel : ScalarModel {
	classvar <updateRoutines, <updateItems;
	var bus, autoUpdateTime, autoUpdateRoutine;
	
	*initClass {
		updateRoutines = IdentityDictionary();
		updateItems = IdentityDictionary();
		
		CmdPeriod.add({
			updateRoutines.do({
				| r |
				r.stop;
			});
			updateRoutines.clear;
			
			updateItems.do( _.clear );
			updateItems.clear;
		})
	}
	
	*new {
		| bus, spec |
		^super.new.initBusModel( bus, spec );
	}
	
	initBusModel {
		| inBus, inSpec |
		bus = inBus;
		spec = inSpec ? spec;
	}
	
	value_{
		| inValue |
		if( value != inValue, {
			value = spec.constrain( inValue );
			bus.set( value );
			valueSignal.changed(value, spec);
		})
	}
	
	autoUpdate {
		| time=0.2 |
		
		// If changing, remove old update routines
		if( time != autoUpdateTime && autoUpdateTime.notNil, {
			updateItems[autoUpdateTime].remove( this );
			if( updateItems[autoUpdateTime].isEmpty, {
				updateRoutines[autoUpdateTime].stop.reset;
				updateRoutines[autoUpdateTime] = nil;
				updateItems[autoUpdateTime].isEmpty
			})
		});
		autoUpdateTime = time;

		if( time.isNumber and: {time>0}, {
			
			if( updateItems[time].isNil, {
				updateItems[time] = IdentitySet();
			});
			
			if( updateRoutines[time].notNil, {
				updateRoutines[time].stop.reset;
				updateRoutines[time] = nil;
			});
			
			updateItems[time].add( this );
			
			updateRoutines[time] = Routine({
				loop {
					updateItems[time].do( _.updateFromServer );
					time.wait;
				}
			}).play;
		})
	}
	
	updateFromServer {
		bus.get({
			| inValue |
			if( value != inValue, {
				value = spec.constrain( inValue );
				valueSignal.changed(value, spec);
			})
		})
	}
}

+Bus {
	asModel {
		^BusModel( this )
	}
}