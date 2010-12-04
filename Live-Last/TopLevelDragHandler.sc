TopLevelDragHandler {
	var view, <dragLayer, selectionManager, dragTargets, dragging, draggingOver;
	
	*new {
		| view, selectionManager |
		^super.new.init(view, selectionManager)
	}	
	
	init {
		| inView, inSelectionManager |
		dragLayer = SCCompositeView( inView, inView.bounds.moveTo(0,0))
			.resize_(5);
		view = inView;
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
					target.containsNode(node);
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
			"start drag".postln;
			
			dragging = List.new(selectionManager.selection.size)
				.addAll(selectionManager.selection);
		})
	}
	
	dragToChanged {
		| x, y |
		var dropPosition, nearestY;
		dragTargets.do({
			| target |
			if (target.bounds.contains(x@y), {
				if (draggingOver != target, {
					draggingOver.notNil.if({ 
						draggingOver.dragToChanged(nil,nil,nil);
					});
					draggingOver = target;
				});
				
				target.dragToChanged( dragging, x, y );
				#nearestY, dropPosition = draggingOver.findDragInsertPoint(y - draggingOver.absoluteBounds.top);
				dropPosition.postln;
			})			
		})
	}
	
	endDrag {
		| x, y |
		var nearestY, dropPosition, draggedItems;
			
		draggingOver.notNil.if({
			#nearestY, dropPosition = draggingOver.findDragInsertPoint(y - draggingOver.absoluteBounds.top);
				
			draggingOver.dragToChanged(nil,nil,nil);
			// @todo - this will have to be abstracted more, since we might not be inserting nodes in the top-most list
			
			if( dropPosition.notNil, {
				draggingOver.updateAfter({
					draggedItems = dragging.collect( _.data );
					dragging.reverse.do({
						| node |
						if( node.data.state != \playing, {
							draggingOver.nodes[node.data].isNil.if({
								node.data.changed(\childRemoved);
								draggingOver.data.insert( dropPosition, node.data );
								draggingOver.data.validate;
							},{
								draggingOver.data.moveTo( dropPosition, node.data );
								draggingOver.data.validate;
							})		
						},{
							dragging.remove(node);
							draggedItems.remove(node.data);
						})
					});
					draggingOver.data.changed(\itemsAdded, draggedItems, dragging )
				});			
			})
		});
	}
}
