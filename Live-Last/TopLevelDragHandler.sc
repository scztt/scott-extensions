TopLevelDragHandler {
	var view, selectionManager, dragTargets, dragging, draggingOver;
	
	*new {
		| view, selectionManager |
		^super.new.init(view, selectionManager)
	}	
	
	init {
		| inView, inSelectionManager |
		selectionManager = inSelectionManager;
		view = inView;
		dragTargets = LinkedList.new;
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
						draggingOver.nodes[node.data].isNil.if({
							node.data.changed(\childRemoved);
							draggingOver.data.insert( dropPosition, node.data );
						},{
							draggingOver.data.moveTo( dropPosition, node.data );
						})		
					});
					draggingOver.data.changed(\itemsAdded, draggedItems, dragging )
				});			
			})
		});
	}
}
