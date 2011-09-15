TopLevelDragHandler {
	classvar <handlers;
	var window, selectionManager, dragTargets, dragging, draggingOver;
	
	*initClass {
		handlers = IdentityDictionary.new;
	}
	
	*get {
		| window |
		^handlers[window.identityHash]
	}
	
	*new {
		| window, selectionManager |
		^super.new.init(window, selectionManager)
	}	
	
	init {
		| inWindow, inSelectionManager |
		this.class.handlers[inWindow.identityHash] = this;
		window = inWindow;
		selectionManager = inSelectionManager;
		dragTargets = LinkedList.new;
	}
	
	getDragParentsMap{
		var contained;
		var result = IdentityDictionary.new;
		var draggingCopy = LinkedList.newFrom(dragging);
		
		dragTargets.do({
			| target |
			if( draggingCopy.isEmpty.not, {
				contained = draggingCopy.removeAllSuchThat({
					| node |
					target.containsNode(node.data);
				});
				contained.do({
					| node |
					result[node] = target;
				});
			})
		});
		
		^result;
	}
	
	addTarget {
		| inTarget |
		dragTargets = dragTargets.add( inTarget );
	}
	
	removeTarget {
		| inTarget |
		dragTargets.remove( inTarget );
	}
	
	 startDrag {
		 if(selectionManager.selection.notEmpty, {
			
			dragging = List.new(selectionManager.selection.size)
				.addAll(selectionManager.selection);
		})
	}
	
	dragToChanged {
		| x, y |
		var dropPosition, nearestY, depth=0, dropTarget;
		dragTargets.do({
			| target |
			if (target.notNil and: { target.absoluteBounds.contains(x@y) }, {
				if( target.depth >= depth, {
					depth = target.depth;
					dropTarget = target;
				});
			})			
		});
		
		if (draggingOver != dropTarget, {
			draggingOver.notNil.if({ 
				draggingOver.dragToChanged(nil,nil,nil);
			});
			draggingOver = dropTarget;
		});
		
		if( dropTarget.notNil, {
			dropTarget.dragToChanged( dragging, x, y );
			#nearestY, dropPosition = dropTarget.findDragInsertPoint(y - dropTarget.absoluteBounds.top);
		})
	}
	
	endDrag {
		| x, y |
		var nearestY, dropPosition, draggedItems, draggingParents, parent;
			
		draggingOver.notNil.if({
			#nearestY, dropPosition = draggingOver.findDragInsertPoint(y - draggingOver.absoluteBounds.top);
				
			draggingOver.dragToChanged(nil,nil,nil);
			// @todo - this will have to be abstracted more, since we might not be inserting nodes in the top-most list
			
			if( dropPosition.notNil, {
				draggingOver.updateAfter({
					draggedItems = dragging.collect( _.data );
					draggingParents  = this.getDragParentsMap();
					dragging.reverse.do({
						| node |
						if( node.data.state != \playing, {
							draggingOver.nodes[node.data].isNil.if({
								parent = draggingParents[node];
								parent.data.seq.remove(node.data).isNil.if({ "nothing removed".warn });
								
								parent.data.changed( \itemsRemoved, [node.data]);
								
								draggingOver.data.seq.insert( dropPosition, node.data );
								draggingOver.data.changed( \itemsAdded, [node.data], [node]);
								draggingOver.data.seq.validate;
							},{
								draggingOver.data.seq.moveTo( dropPosition, node.data );
								draggingOver.data.seq.validate;
							})		
						},{
							// dragging.remove(node);
							// draggedItems.remove(node.data);
						})
					});
					draggingOver.data.changed(\itemsAdded, draggedItems, dragging )
				});			
			})
		});
	}
}
