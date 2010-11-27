LinkedObjectList : LinkedList {
	tail { ^tail }
	head { ^head }
	
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
}
