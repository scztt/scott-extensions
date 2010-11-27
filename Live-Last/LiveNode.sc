LiveNode {
	var <data, <view, <>dragHandler, <>selectionManager, <>parent;
	var dragging=false, dragPoint, viewsToDelete;
	var color, <>dragLayer, drawSelected=false, removeInFuture;
	
	*new{
		| selectionManager |
		^super.new.initLiveNode(selectionManager);
	}
	
	initLiveNode {
		| inSelectionManager |
		selectionManager = inSelectionManager;
		color = Color.rand;
		viewsToDelete = LinkedList.new;
	}
	
	data_{
		| inData |
		if( data.notNil, {
			data.removeDependant(this);
		});
		data = inData;
		data.addDependant(this);			
	}
	
	createView {
		| parent, rect |
		"creating view".postln;
		view = SCUserView( parent, rect );
		view.drawFunc = this.draw(_);
		view.mouseDownAction = { |view,x,y,mod| this.mouseDown(view,x,y, mod) };
		view.mouseMoveAction = { |view,x,y,mod| this.mouseMove(view,x,y,mod) };
		view.mouseUpAction = { |view,x,y,mod| this.mouseUp(view,x,y,mod) };
		view.canFocus_(false);
	}
	
	draw {
		| inView |
		Pen.fillColor = color;
		Pen.strokeColor = Color.black.alpha_(0.5);
		Pen.joinStyle = 1;
		Pen.width = 3;
		Pen.fillRect( this.bounds.moveTo(0,0) );
		if( drawSelected, { 
			Pen.strokeRect( this.bounds.moveTo(0,0) );
		})
	}
	
	select {
		drawSelected=true;
		view.refresh();
	}
	
	deselect {
		drawSelected = false;
		view.refresh();
	}
	
	mouseDown {
		| inView, x, y, modifiers |
		modifiers.postln;
		if( drawSelected.not && (modifiers & KeyCodeResponder.shiftModifier != KeyCodeResponder.shiftModifier), {
			selectionManager.setSelection(this);
		},{
			selectionManager.addSelection(this);		
		});
			
		dragPoint = x@y;
	}
	
	mouseMove {
		| inView, x, y |
		var absBounds;
		if( dragging, {
			absBounds = inView.absoluteBounds;
			dragHandler.dragToChanged( x+absBounds.left,y+absBounds.top );
		},{
			dragging = true;
			dragHandler.startDrag([this]);		
		})
	}
	
	mouseUp {
		| inView, x,y |
		var absBounds;
		if( dragging, {
			absBounds = inView.absoluteBounds;
			dragging = false;
			dragHandler.endDrag(x+absBounds.left,y+absBounds.top);
		})
	}
	
	moveTo {
		| inPoint |
		this.bounds = this.bounds.moveTo( *(inPoint.asArray) )
	}
	
	height {
		^this.bounds.height;
	}
	
	bounds {
		^view.bounds;
	}
	
	bounds_{
		| inBounds |
		view.bounds = inBounds;
	}
	
	update {
		| changed, what, args |
	}
	
	removeView {
		var forRemoval;
		"removing view".postln;
		if( view.notNil && dragging.not, {
			forRemoval = view;
			view.visible = false;
			{ 
				forRemoval.remove();
			}.defer(0.1);
			view = nil;
			removeInFuture = false;
		},{
			"removing in future...".postln;
			removeInFuture = true;
		})		
	}
}