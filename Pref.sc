/*
Pref : Pattern {
	var <>list, <>repeats=1;
	var <>offset;
	
		
	*new { arg list, repeats=1, offset=0;
		^super.new.list_(list).repeats_(repeats).offset_(offset)
	}
	
	copy {
		^super.copy.list_(list.copy)
	}
	
	embedInStream {  arg inval;
		var item, offsetValue;
		offsetValue = offset.value;
		if (inval.eventAt('reverse') == true, {
			repeats.value.do({ arg j;
				list.value.size.reverseDo({ arg i;
					item = list.value.wrapAt(i + offsetValue);
					inval = item.embedInStream(inval);
				});
			});
		},{
			repeats.value.do({ arg j;
				list.value.size.do({ arg i;
					item = list.value.wrapAt(i + offsetValue);
					inval = item.embedInStream(inval);
				});
			});
		});
		^inval;
	}

	storeArgs { ^[ list, repeats, offset ] }
}
*/

/*
ListProxy : FuncProxy {
	
	size {
		^value.size;	
	}
}
*/