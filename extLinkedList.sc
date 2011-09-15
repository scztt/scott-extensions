+ List {
	moveTo {
		| obj, newIndex |
		var oldIndex = this.indexOf(obj);
		this.insert( newIndex, obj );
		if( newIndex < oldIndex, {
			this.removeAt( oldIndex+1 );
		},{
			this.removeAt( oldIndex );
		})
	}
}


+ LinkedList {

	headNode {
		^head
	}
	
	tailNode {
		^tail
	}

	indexOf { arg item;
		this.do({ arg elem, i;
			if ( item === elem, { ^i })
		});
		^nil
	}
	
	// remove and return index
	removeRIndex {
		| item |
		var node = head;
		size.do({
			|n|
			if( node.obj == item, {
				if (head == node, { head = node.next; });
				if (tail == node, { tail = node.prev; });
				node.remove;
				size = size - 1;
				^n
			});
			node = node.next;
		});
		^nil
	}

	insert {
		| index, item |
		var oldItem, newItem;
		if( index >= (size), {
			this.add( item );
		},{
			if( index<=0, { this.addFirst(item) }, {
				oldItem = this.nodeAt( index );
				newItem = LinkedListNode( item );
				newItem.prev = oldItem.prev;
				newItem.next = oldItem;
				oldItem.prev.next = newItem;
				oldItem.prev = newItem;
				size = size+1;
			})
		})
	}
	
	insertAll {
	}
	
	// This is unoptimized, and could probably be faster. Uh.
	moveToAfter {
		| oldIndex, newIndex |
		var  oldNode;
		
		this.insert( newIndex+1, this.at( oldIndex ) );

		if( newIndex < oldIndex, {
			this.removeAt( oldIndex+1 ); 
		},{
			this.removeAt( oldIndex );
		})
	}
	
	removeNilItems {
		var removed=0, iter, toRemove, end;
		iter = head;
		while({ iter.notNil }, {
			if( iter.obj.isNil, {
				toRemove = iter;
				if( head==toRemove,{
					head = toRemove.next;
				});
				if( tail==toRemove, {
					tail = toRemove.prev;
				});
				iter = iter.next;
				toRemove.remove();
				removed = removed + 1;
			},{
				iter = iter.next;
			})			
		});
		size = size - removed;
	}
	
	validate {
		var iter, end, count=0;
		if( (head.notNil && tail.isNil) || (head.isNil && tail.notNil), {
			"head is %, tail is %.\n".postf( head, tail );
			^false;
		});
		
		iter = head;
		while( { iter.notNil && (count<(size+1)) }, {
			if( (iter.prev.isNil) && (iter!=head), {
				"item % prev is nil.\n".postf(count);
				^false;
			});
			if( (iter.next.isNil) && (iter!=tail), {
				"item % next is nil.\n".postf(count);
				^false
			});
			count = count+1;
			iter = iter.next;
		});
		if( count != size, {
			"count=%, size=%.\n".postf( count, size );
			^false;
		})
	}
}