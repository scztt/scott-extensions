LiveNode {
	var <data, <view, <detailView, <>dragLayer, parent, <>selectionManager;
	var <>mouseDownAction, <>mouseUpAction, <>mouseMoveAction;
	var dragging=false, dragPoint,
		expanded=false;
	var color, drawSelected=false, headerSize=25, removeInFuture;
	var uninitializedColor, initializedColor, preparedColor, playingColor;
	
	*new{
		| selectionManager |
		^super.new.initLiveNode(selectionManager);
	}
	
	initLiveNode {
		| inSelectionManager |
		selectionManager = inSelectionManager;
		color = Color.hsv(0.6,0.3,0.7);
		
		initializedColor = Color.grey();
		uninitializedColor = Color.hsv( Color.red.asHSV[0], 0.7, 0.9 );
		preparedColor = Color.hsv( Color.yellow.asHSV[0], 0.7, 0.9 );
		playingColor = Color.hsv( Color.green.asHSV[0], 0.7, 0.9 );
	}
	
	data_{
		| inData |
		if( data.notNil, {
			data.removeDependant(this);
		});
		if( inData.notNil, {
			color = inData.color ? Color.hsv(0.6,0.3,0.7);
		});
		data = inData;
		data.addDependant(this);
	}
	
	createView {
		| inParent, inRect |
		parent = inParent;
		view = SCUserView( inParent, inRect );
		
		view.drawFunc = this.draw(_);
		view.mouseDownAction = { |view,x,y,mod| this.mouseDown(view,x,y, mod) };
		view.mouseMoveAction = { |view,x,y,mod| this.mouseMove(view,x,y,mod) };
		view.mouseUpAction = { |view,x,y,mod| this.mouseUp(view,x,y,mod) };
		view.canFocus_(false);		
	}
	
	draw {
		| inView |
		var icon;
		Pen.fillColor = color;
		
		if( drawSelected, { 
			Pen.strokeColor = Color.black.alpha_(1);
			Pen.width = 1.5;
		},{
			Pen.strokeColor = Color.black.alpha_(0.5);
			Pen.width = 0.5;
		});
		
		Pen.roundedRect( this.bounds.moveTo(0,0).insetBy(1.5), 1.5 ).fillStroke;

		switch (data.state)
			{ \initialized }
				{
					Pen.color = initializedColor;
					Pen.fillRect( Rect(0,0,25,25).insetBy(6) );
				}
			{ \preparing }
				{
					Pen.color = preparedColor.alpha_(0.5);
					Pen.fillRect( Rect(0,0,25,25).insetBy(6) );
				}
			{ \prepared }
				{
					Pen.color = preparedColor;
					Pen.fillRect( Rect(0,0,25,25).insetBy(6) );
				}
			{ \playing }
				{
					Pen.color = playingColor;
					Pen.fillRect( Rect(0,0,25,25).insetBy(6) );
				}
			{ \donePlaying }
				{
					Pen.color = playingColor.alpha_(0.5);
					Pen.fillRect( Rect(0,0,25,25).insetBy(6) );
				}
		;
		icon = expanded.if( {\down}, {\play} );
		Pen.fillColor = initializedColor;
		
		DrawIcon( icon, Rect(this.bounds.right-25, 2, 20, 20) );
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
		if( x > (this.bounds.right-30), {
			this.expandAction();
//			if( (data.state!=\playing), {
//				if( drawSelected.not && (modifiers & KeyCodeResponder.shiftModifier != KeyCodeResponder.shiftModifier), {
//					selectionManager.setSelection(this);
//				},{
//					selectionManager.addSelection(this);		
//				});
//			});
		},{
			mouseDownAction.(inView, x, y, modifiers);
		});
			
		dragPoint = x@y;
	}
	
	mouseMove {
		| inView, x, y |
		var absBounds = inView.absoluteBounds;
//		mouseMoveAction.(this, );
//		if( (data.state!=\playing), {
//			if( dragging, {
//				absBounds = inView.absoluteBounds;
//				// dragHandler.dragToChanged( x+absBounds.left,y+absBounds.top );
//				dragAction.value( this, x+absBounds.left, y+absBounds.top );
//			},{
//				dragging = true;
//				// dragHandler.startDrag([this]);		
//				startDragAction.value( this );
//			})
//		})
	}
	
	mouseUp {
		| inView, x,y |
		mouseUpAction.(inView, x, y);
//		var absBounds;
//		if( dragging, {
//			absBounds = inView.absoluteBounds;
//			dragging = false;
//			//dragHandler.endDrag();
//			endDragAction.value( this, x+absBounds.left, y+absBounds.top );
//		})
	}
	
	expandAction {
		var viewBounds, childBounds;
		expanded = expanded.not;
		if( view.notNil, {
			viewBounds = this.view.bounds;
			if( expanded, {
				if( data.hasGui,{
					detailView = SCCompositeView( parent, viewBounds.insetBy(1, 0).moveBy(0, headerSize) );
					data.makeDetailView( detailView, detailView.bounds.moveTo(0,0), selectionManager );
					detailView.children.do({
						| child |
						child.bounds.postln;
						viewBounds.height_(max(viewBounds.height, child.bounds.bottom));
					});
					detailView.bounds = detailView.bounds.height_(viewBounds.height);
					detailView.refresh();
				})
			},{
				detailView.notNil.if({
					detailView.remove();
					detailView = nil;
				})
			});
		});
		this.changed(\boundsChanged);
	}
	
	moveTo {
		| inPoint |
		this.bounds = view.bounds.moveTo( *(inPoint.asArray) )
	}
	
	height {
		^this.bounds.height;
	}
	
	bounds {
		if( expanded, {
			^view.bounds.union( detailView.bounds );
		}, {
			^view.bounds;
		})
	}
	
	bounds_{
		| inBounds |
		if( inBounds.height != view.bounds.height, {
			"bounds height changed from % to %\n".postf( view.bounds.height, inBounds.height );
		});
		view.bounds = inBounds;
	}
	
	update {
		| who, what, args |
		
		switch (what)
			{ \stateChanged }
				{
					{ 
						if( drawSelected && (args==\playing), {
							selectionManager.removeSelection(this);
						});
						this.view.notClosed.if({
							this.view.refresh() 
						});
					}.defer;
				}
		;	
	}
	
	removeView {
		var forRemoval = List.new;
		
		if( dragging.not, {
			if( view.notNil, {
				forRemoval.add(view);
				view.visible = false;
			});
			if( detailView.notNil, {
				forRemoval.add(detailView);
				detailView.visible = false;
			});		
			{ 
				forRemoval.do(_.remove());
				forRemoval.clear;
			}.defer(0.1);
			view = detailView = nil;
			removeInFuture = false;
		},{
			removeInFuture = true;
		});
	}
}