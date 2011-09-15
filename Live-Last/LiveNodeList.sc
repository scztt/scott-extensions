LiveNodeListView : View {
	var topMargin = 3, leftMargin = 3, verticalSpacing = 1;
	var <data, <nodes, dragLayer, decoratorLayer, contentLayer, dragHighlight, dragHighlightPosition,  
		<dragHandler, dragEvent, draggedItems,
		<blockUpdates=false, needsUpdate=false,
		<>selectionManager, <>topLevel = true, <>drawBorders=true,
		backgroundColor, lineColor, highlightColor, <>depth=0;
	
//	*viewClass { ^CompositeView }
	
	*new {
		| parent, bounds, topLevel=true |
		^super.new(parent, bounds).topLevel_(topLevel).initLiveNodeList();
	}
	
	initLiveNodeList {
		nodes = IdentityDictionary.new;
		decoratorLayer = UserView( this, this.bounds.moveTo(0,0))
			.drawFunc_({ this.decoratorDraw() });
		contentLayer = CompositeView( this, this.bounds.moveTo(0,0));
		dragLayer = CompositeView( this, this.bounds.moveTo(0,0));
		
		dragHandler = TopLevelDragHandler.get(this.findWindow);
		if( topLevel, {
			dragHandler.addTarget( this );
		});
		
		backgroundColor = Color.grey(0.7);
		lineColor = Color.grey(0.3);
		highlightColor = Color.yellow.alpha_(0.9);
		
		this.focusColor = Color.clear;
		
		this.onClose = {
			nodes.do({
				| node |
				node.data = nil;
				//node.removeView();
			});
			dragHandler.removeTarget( this );
			data.removeDependant(this);
			nodes = dragHandler = dragEvent = draggedItems = selectionManager = nil;
			this.releaseDependants();
		}
	}
	
	dragHandler_{
		| inHandler |
		dragHandler = inHandler;
		dragHandler.addTarget( this );
	}
	
	data_{
		| inData |
		if( data.notNil, {
			data.removeDependant(this);
		});
		data = inData;
		data.addDependant(this);	
		
		this.clearAllNodes();
		this.rebuild();
	}
	
	clearAllNodes {
		nodes.values.do({
			| node |
			node.removeDependant(this);
			node.removeView();
		});
		nodes.clear;
	}
	
	rebuild {
		var differenceSet, node;
		var newNodes = IdentityDictionary.new;
		data.seq.do({
			| item |
			node = nodes[item];
			node.notNil.if({
				newNodes[item] = node;
			},{
				newNodes[item] = this.createNode(item);
				this.addNode( item, newNodes[item] )
			});
		});
		differenceSet = nodes.values.difference( newNodes.values );
		differenceSet.do({
			| node |
			// do removal stuff
		});
		nodes = newNodes;
		this.updateArrangement();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Dragging
	
	nodeMouseDownAction {
		| node, x, y, modifiers |
		if( node.drawSelected.not && (modifiers & KeyCodeResponder.shiftModifier != KeyCodeResponder.shiftModifier), {
			selectionManager.setSelection(node);
		},{
			selectionManager.addSelection(node);		
		});
	}

	nodeMouseMoveAction {
		| node, x, y, modifiers |
		var absBounds;
		if( node.dragging, {
			absBounds = node.absoluteBounds;
			dragHandler.dragToChanged( x+absBounds.left,y+absBounds.top );
		},{
			node.dragging = true;
			dragHandler.startDrag();		
		})
	}

	nodeMouseUpAction {
		| node, x, y, modifiers |
		var absBounds;
		if( node.dragging, {
			absBounds = node.absoluteBounds;
			node.dragging = false;
			dragHandler.endDrag( x+absBounds.left, y+absBounds.top);
		})
	}
	
	containsNode {
		| node |
		^nodes[node].notNil
	}
	
	dragToChanged {
		| items, x, y |
		var nearestY, nearestYDelta, dropPosition, dropY, nodeUnder;
				
		if( items.notNil, {
			dropY = y - this.absoluteBounds.top;
			nodes.do({
				| node |
				if( node.absoluteBounds.contains( x@y ), {
					if( node.acceptsDrag, {
						if( node.dragToChanged( items, x, y ), {
							^true
						});
					})
				})
			});
			#nearestY, dropPosition = this.findDragInsertPoint(dropY);
			this.putDragHighlight( nearestY );
			^true;
		},{
			this.removeDragHighlight();
		})
	}
	
	findDragInsertPoint {
		| y |
		var nearestY, dropPosition, nearestYDelta;
		var node, nodeUnder;
		if (data.seq.size>0, {
			node = nodes[data.seq[0]];
			nearestY = node.bounds.top-2;
			nearestYDelta = (nearestY-y).abs;
			dropPosition = 0;
			data.seq.do({
				| item, i |
				var bounds, delta;
				bounds = nodes[item].bounds;
				delta = (y-(bounds.top+bounds.height)).abs;
				if( delta < nearestYDelta, {
					nearestYDelta = delta;
					nearestY = bounds.top+bounds.height;
					dropPosition = i+1;
				});
			});
			^[nearestY, dropPosition];		
		},{
			^[topMargin, 0];
		});
		^[nil, nil, nodeUnder];
	}
	
	putDragHighlight {
		| y |
		if( y.notNil, {
			dragHighlightPosition = y+1;
			dragHighlight = dragHighlight ?? { 
				UserView( dragLayer, this.bounds.moveTo(0,0)) 
					.drawFunc_({ 
						| view |
						var bounds = view.bounds.moveTo(0,0).insetBy(3,3);
						Pen.strokeColor = highlightColor;
						Pen.width = 3;
						//Pen.strokeRect( bounds );
						Pen.line( 3@dragHighlightPosition, bounds.right@dragHighlightPosition );
						Pen.stroke;
					})
			};
			dragHighlight.refresh();
		},{
			this.removeDragHighlight();
		})
	}
	
	removeDragHighlight {
		dragHighlight.notNil.if({ dragHighlight.remove() });
		dragHighlight = nil;
		nil.notNil.if({
			this.refresh();
		});
	}
	
	decoratorDraw {
		if( drawBorders, {
			Pen.width = 4;
			Pen.color = backgroundColor;
			Pen.fillRect( decoratorLayer.bounds.insetBy(3) );
			Pen.color = lineColor;
			Pen.strokeRect( decoratorLayer.bounds );
		})
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Nodes
	
	createNode {
		| data, update=true |
		var newNode = LiveNode( selectionManager );
		newNode.data = data;
		//this.initNode( newNode );
		^newNode;
	}
	
	initNode {
		| newNode |
		newNode.dragLayer = dragLayer;
		//newNode.dragHandler = dragHandler;
		newNode.createView( contentLayer, Rect(0,0,this.bounds.width-leftMargin-leftMargin,24));
		
		newNode.mouseDownAction = { |...args| this.nodeMouseDownAction(*args) };
		newNode.mouseMoveAction = { |...args| this.nodeMouseMoveAction(*args) };
		newNode.mouseUpAction = { |...args| this.nodeMouseUpAction(*args) };
		
		newNode.addDependant( this );
	}
	
	postNodelist {
		nodes.do({
			|node|
			"\t% ".postf(node);
			node.notNil.if({  node.bounds.top.post  });
			"\n".postln;
		})
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Nodelist management
	
	addNode {
		| item, node |
		node.depth = depth+1;
		if(nodes[item].notNil, {
			"Already a node for this item!".warn;
			^false;
		});
		
		nodes[item] = node;
		this.initNode( node );		
	}
	
	removeNode {
		| item |
		var node = nodes.removeAt(item);
		node.removeView();
		node.removeDependant( this );
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Signals
	
	update {
		| who, what ...args |
		var node, nodes, items;
		
		switch (what)
		{ \itemsAdded }
			{
				items = args[0];
				nodes = args[1];
				if(nodes.isNil, {
					items.do({
						| item |
						node = this.createNode(item);
						this.addNode( item, node )
					})
				},{
					items.do({
						| item, i |
						this.addNode( item, nodes[i] )
					})					
				});
				
				this.updateArrangement();
			}
		{ \itemsRemoved }
			{
				args[0].do({
					| item |
					this.removeNode( item );
				});
				this.updateArrangement();
			}
		{ \listChanged }
			{
				this.rebuild();
				this.updateArrangement();
			}
		{ \boundsChanged }
			{
				this.updateArrangement();
			}
		;
	}
	
	updateAfter {
		| func |
		this.blockUpdates = true;
		func.value;
		this.blockUpdates = false;
	}
	
	blockUpdates_{
		| inBlock |
		blockUpdates = inBlock;
		if( blockUpdates.not && needsUpdate, {
			needsUpdate = false;
			this.updateArrangement();
		})
	}
	
	updateArrangement {
		var node, y = topMargin;
		if(blockUpdates, {
			needsUpdate = true;
		},{
			"updating arrangement".postln;
			data.seq.do({
				| item, i |
				node = nodes[item];
				if( node.notNil, {
					node.moveTo(leftMargin@y);
					y = y + node.height.postln + verticalSpacing;
				},{
					"Nil node encountered....".warn;
				})
			});
			
			y = y + verticalSpacing + 1;
			this.bounds = this.bounds.height_(y);
			this.refresh();
			this.changed(\boundsChanged);
		})
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Etc.
	
	bounds_{
		| rect |
		this.setProperty(\bounds, rect);
		dragLayer.bounds = rect.moveTo(0,0);
		contentLayer.bounds = rect.moveTo(0,0);
		decoratorLayer.bounds = rect.moveTo(0,0);
	}
	
	checkNodes {
		nodes.do({
			| node, i |
			"%: %\n".postf(i, node.view.slotAt(0));
		})
	}
}