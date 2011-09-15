LiveNode {
	var <data, <view, <detailView, <detailViewContents, <>dragLayer, parent, <>selectionManager, originalBounds;
	var <>mouseDownAction, <>mouseUpAction, <>mouseMoveAction;
	var <>dragging=false, <>dragPoint,
		expanded=false, <acceptsDrag=false;
	var color, <>drawSelected=false, headerSize=25, removeInFuture;
	var uninitializedColor, initializedColor, preparedColor, playingColor, <>depth=0;
	
	*new{
		| selectionManager |
		^super.new.initLiveNode(selectionManager);
	}
	
	initLiveNode {
		| inSelectionManager |
		selectionManager = inSelectionManager;
		
		initializedColor = Color.grey();
		uninitializedColor = Color.hsv( Color.red.asHSV[0], 0.7, 0.9 );
		preparedColor = Color.hsv( Color.yellow.asHSV[0], 0.7, 0.9 );
		playingColor = Color.hsv( Color.green.asHSV[0], 0.7, 0.9 );
	}
	
	data_{
		| inData |
		var inColor;
		if( data.notNil, {
			data.removeDependant(this);
		});
		if( inData.notNil, {
			inColor = inData.color;
			color = if( inColor.isNil, {
				Color.hsv(0.7, 0.4, 0.8)
			},{
				if( inColor.isNumber, {
					Color.hsv(inColor, 0.4, 0.8);
				},{
					inColor.value;
				})
			})
		});
		data = inData;
		data.addDependant(this);
	}
	
	createView {
		| inParent, inRect |
		parent = inParent;
		originalBounds = inRect;
		view = UserView( inParent, inRect );
		
		view.drawFunc = this.draw(_);
		view.mouseDownAction = { |view,x,y,mod| this.mouseDown(view,x,y, mod) };
		view.mouseMoveAction = { |view,x,y,mod| this.mouseMove(view,x,y,mod) };
		view.mouseUpAction = { |view,x,y,mod| this.mouseUp(view,x,y,mod) };
		view.canFocus_(false);		
		view.onClose_({ 
			| inView |
			//view = nil 
		});
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
					Pen.color = playingColor.alpha_(0.75);
					Pen.fillRect( Rect(0,0,25,25).insetBy(6) );
				}
		;
		
		if(data.hasDetailView, {
			icon = expanded.if( {\down}, {\play} );
			Pen.fillColor = initializedColor;
			
			DrawIcon( icon, Rect(this.bounds.right-25, 2, 20, 20) );
		});
		
		if( data.name.notNil, {
			Pen.strokeColor = Color.black;
			Pen.fillColor = Color.black;
			Pen.font = Font( "Helvetica", 10);
			Pen.stringCenteredIn( data.name, originalBounds.moveTo(0,0).insetBy(3.5)  )
		})
	}
	
	select {
		drawSelected=true;
		view.refresh();
	}
	
	deselect {
		drawSelected = false;
		if( view.notNil, { view.refresh() })
	}
	
	mouseDown {
		| inView, x, y, modifiers |
		if( (x > (this.bounds.right-30)) && data.hasDetailView, {
			this.expandAction();
//			if( (data.state!=\playing), {
//				if( drawSelected.not && (modifiers & KeyCodeResponder.shiftModifier != KeyCodeResponder.shiftModifier), {
//					selectionManager.setSelection(this);
//				},{
//					selectionManager.addSelection(this);		
//				});
//			});
		},{
			mouseDownAction.(this, x, y, modifiers);
		});
			
		dragPoint = x@y;
	}
	
	mouseMove {
		| inView, x, y |
		var absBounds = inView.absoluteBounds;
		mouseMoveAction.(this, x, y);
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
		mouseUpAction.(this, x, y);
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
				if( data.hasDetailView,{
					detailView = CompositeView( parent, viewBounds.moveBy(0, headerSize) );
					// detailView.background_(Color.red.alpha_(0.3));
					detailViewContents = data.makeDetailView( detailView, detailView.bounds.moveTo(0,0).insetBy(2,0), selectionManager );
					detailViewContents.addDependant(this);
					detailViewContents.respondsTo(\depth_).if({
						detailViewContents.depth = this.depth;
					},{
						"cannot set depth".postln;
					});
					detailView.children.do({
						| child |
						viewBounds.height_(max(viewBounds.height, child.bounds.bottom));
					});
					detailView.bounds = detailView.bounds.height_(viewBounds.height);
					detailView.onClose = { 
						detailViewContents.releaseDependants();
						detailView = nil;
					};
					this.updateBounds();
					detailView.refresh();
					acceptsDrag = data.detailAcceptsDrag;
				})
			},{
				detailView.notNil.if({
					detailViewContents.releaseDependants();
					detailView.remove();
					detailView = detailViewContents = nil;
					acceptsDrag = false;
					this.updateBounds();
				})
			});
		});
	}
	
	updateBounds {
		if( detailView.notNil, {
			this.detailView.bounds = this.detailView.bounds.height_(detailViewContents.bounds.height+2);
			this.bounds = originalBounds.copy.height_(headerSize+detailViewContents.bounds.height+2);
			this.changed(\boundsChanged);
		},{
			this.bounds = originalBounds;
			this.changed(\boundsChanged);
		})
	}
	
	moveTo {
		| inPoint |
		this.bounds = view.bounds.moveTo( *(inPoint.asArray) );
		if( detailView.notNil, {
			detailView.bounds_( detailView.bounds.moveTo( inPoint.x, headerSize+inPoint.y ) );
		})
	}
	
	height {
		^this.absoluteBounds.height;
	}
	
	bounds {
		if( view.isNil, {
			"view is nil.... oops".postln;
			^Rect(0,0,0,0);
		});
		if( expanded, {
			^view.bounds;
		}, {
			^view.bounds;
		})
	}
	
	absoluteBounds {
		if( view.isNil, {
			"view is nil.... oops".postln;
			^Rect(0,0,0,0);
		});
		if( expanded, {
			"view: %\n".postf(view.absoluteBounds);
			"detailViewContents: %\n".postf(detailViewContents.absoluteBounds);
			^view.absoluteBounds.union( detailViewContents.absoluteBounds );
		}, {
			^view.absoluteBounds;
		})
	}
	
	bounds_{
		| inBounds |
		if( inBounds.height != view.bounds.height, {
			"bounds height changed from % to %\n".postf( view.bounds.height, inBounds.height );
		});
		view.bounds = inBounds;
	}
	
	dragToChanged {
		| ...args |
		if( detailView.notNil && acceptsDrag, {
			^detailViewContents.dragToChanged(*args);
		},{
			^false
		})
	}
	
	update {
		| who, what, args |
		
		switch (what)
			// Child item bounds changed - pass it along
			{ \boundsChanged }
				{
					"child bounds changed".postln;
					this.updateBounds();
				}
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
				forRemoval.do({ |item| item.remove() });
				forRemoval.clear;
			}.defer(0.1);
			forRemoval.do({ |i| i.identityHash.postln });
			view = detailView = nil;
			selectionManager.removeSelection(this);
			removeInFuture = false;
		},{
			"deferring removal for a later date.".postln;
			removeInFuture = true;
		});
	}
}