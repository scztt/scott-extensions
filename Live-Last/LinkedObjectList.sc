LinkedObjectListNode {
	var <>prev, <>next;

	*new { ^super.new() }

	obj { ^this }

	obj_{ 
		"Cannot set obj for LinkedObjectListNode's.".warn;
		this.dumpBackTrace;
	}

	remove {
		if (prev.notNil, { prev.next_(next); });
		if (next.notNil, { next.prev_(prev); });
		next = prev = nil;
	}	
}

LinkedObjectList : LinkedList {
	var <>prev, <>next;
	
	tail { ^tail }
	head { ^head }
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	// LinkedObjectListNode
	obj { ^this }
	obj_{ "Cannot set obj for LinkedObjectListNode's.".warn }
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	
	addFirst {
		| obj |
		var node = obj;
		if (head.notNil, {
			node.next_(head);
			head.prev_(node);
		});
		head = node;
		
		if (tail.isNil, {
			tail = node;
		});
		size = size + 1;
	}
	
	add { arg obj;
		var node = obj;
		if (tail.notNil, {
			node.prev_(tail);
			node.next_(tail.next);
			tail.next_(node);
		});
		tail = node;
		if (head.isNil, {
			head = node;
		});
		size = size + 1;
	}
	
	put { arg index, item;
		var node = this.nodeAt(index);
		if (node.notNil, {
			item.next = node.next;
			item.prev = node.prev;
			node.remove();
		});
	}
	
	insert {
		| index, item |
		var oldItem, newItem;
		if( index >= (size), {
			this.add( item );
		},{
			if( index<=0, { this.addFirst(item) }, {
				oldItem = this.nodeAt( index );
				newItem = item;
				newItem.prev = oldItem.prev;
				newItem.next = oldItem;
				oldItem.prev.next = newItem;
				oldItem.prev = newItem;
				size = size+1;
			})
		})
	}
	
	insertAllAfter { arg index, list;
		var node;
		if( list.isKindOf(LinkedObjectList), {
			node = this.nodeAt(index);
			if (node.notNil, {
				if(node==tail, {
					node.next = list.head;
					tail = list.tail;
				},{
					node.next.prev = list.tail;
					list.tail.next = node.next;
					
					list.head.prev = node;
					node.next = list.head;		
				});
			})							
		},{
			var newList = LinkedObjectList();
			list.do({
				| item |
				newList.add(item);
			});
			this.insertAllAfter(index, newList);
		})
	}
	
	moveTo {
		| newIndex, obj |
		var oldIndex = this.indexOf(obj);
		this.remove( obj );
		if( newIndex < oldIndex, {
			this.insert( newIndex, obj );
		},{
			this.insert( newIndex-1, obj );
		});
	}	
	
	findNodeOfObj {
		| obj | ^obj
	}
	
	remove {
		| node |
		if( node.notNil, {
			if (head == node, { head = node.next; });
			if (tail == node, { tail = node.prev; });
			
			if (node.prev.notNil, { node.prev.next_(node.next); });
			if (node.next.notNil, { node.next.prev_(node.prev); });
			
			node.next = node.prev = nil;
			size = size - 1;
			^node;
		})
	}	

}
