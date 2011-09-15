
Pqueue : ListPattern {
	var <>nextPatternAction;
	*new { | list, repeats=1, nextPatternAction=nil |
		^super.new(list, repeats).init( nextPatternAction ); 
	}
	
	*newWithLinkedList { | list, repeats=1, nextPatternAction=nil |
		^super.new([nil], repeats).initWithLinkedList( list, nextPatternAction ); 
	}
	
	init {
		| npa |
		nextPatternAction = npa;
		list.isKindOf( LinkedList ).not.if({
			list = LinkedList.new.addAll( list );
		})
	}
	
	initWithLinkedList {
		| l, npa |
		list = l;
		nextPatternAction = npa;
	}
	
	embedInStream {
		arg event;
		var node, prevNode;
		
		
		repeats.do({
			node = list.nodeAt(0);
			while( { node.isNil.not }, {
				nextPatternAction.value( node, prevNode );
				event = node.obj.embedInStream(event);
				prevNode = node;
				node = node.next;
			});
			nextPatternAction.value( nil, prevNode );
		});
		
		^event;
	}
}	
