LiveNodeList : SCCompositeView {
	var topMargin = 5, leftMargin = 2, verticalSpacing = 2;
	var <data, <nodes, dragLayer, dragHighlight, dragHighlightPosition, contentLayer, 
		<dragHandler, dragEvent, draggedItems,
		<blockUpdates=false, needsUpdate=false,
		<>selectionManager;
	
	*viewClass { ^SCCompositeView }
	
	*new {
		|...args|
		^super.new(*args).initLiveNodeList();
	}
	
	initLiveNodeList {
		nodes = IdentityDictionary.new;
		contentLayer = SCCompositeView( this, this.bounds.moveTo(0,0));
		dragLayer = SCCompositeView( this, this.bounds.moveTo(0,0));	}
	
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
		data.do({
			| item |
			node = nodes[item];
			node.notNil.if({
				newNodes[item] = node;
			},{
				newNodes[item] = this.createNode(item);
			});
		});
		differenceSet = nodes.values.difference( newNodes.values );
		differenceSet.do({
			| node |
			// do removal stuff
		});
		nodes = newNodes;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Dragging
	
	dragToChanged {
		| items, x, y |
		var nearestY, nearestYDelta, dropPosition, dropY;
		
		if( items.notNil, {
			dropY = y - this.absoluteBounds.top;
			#nearestY, dropPosition = this.findDragInsertPoint(dropY);
			this.putDragHighlight( nearestY )
		},{
			this.removeDragHighlight();
		})
	}
	
	findDragInsertPoint {
		| y |
		var nearestY, dropPosition, nearestYDelta;
		var node;
		if (data.size>0, {
			node = nodes[data[0]];
			nearestY = node.bounds.top;
			nearestYDelta = (nearestY-y).abs;
			dropPosition = 0;
			data.do({
				| data, i |
				var bounds, delta;
				bounds = nodes[data].bounds;
				delta = (y-(bounds.top+bounds.height)).abs;
				if( delta < nearestYDelta, {
					nearestYDelta = delta;
					nearestY = bounds.top+bounds.height;
					dropPosition = i+1;
				});
			});
			^[nearestY, dropPosition];		
		});
		^[nil,nil];
	}
	
	putDragHighlight {
		| y |
		if( y.notNil, {
			dragHighlightPosition = y;
			dragHighlight = dragHighlight ?? { 
				SCUserView( dragLayer, this.bounds.moveTo(0,0)) 
					.drawFunc_({ 
						| view |
						var bounds = view.bounds;
						Pen.strokeColor = Color.yellow.alpha_(0.6);
						Pen.width = 2;
						Pen.strokeRect(bounds.moveTo(0,0));
						Pen.strokeColor = Color.yellow.alpha_(0.6);
						Pen.width = 4;
						Pen.line( 0@dragHighlightPosition, bounds.width@dragHighlightPosition );
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
		this.refresh();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Nodes
	
	createNode {
		| data, update=true |
		var newNode = LiveNode( selectionManager );
		newNode.data = data;
		this.initNode( newNode );
		^newNode;
	}
	
	initNode {
		| newNode |
		newNode.dragLayer = dragLayer;
		newNode.dragHandler = dragHandler;
		newNode.createView( contentLayer, Rect(0,0,this.bounds.width-leftMargin-leftMargin,20));
		newNode.addDependant( this );
	}
	
	postNodelist {
		"Nodelist:".postln;
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
		
		switch (what)
		{ \itemsAdded }
			{
				var node, nodes, items;
				"items added.".postln;
				args.postln;
				items = args[0];
				nodes = args[1];
				if(nodes.isNil, {
					items.do({
						| item |
						node = this.createNode(who);
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
				"items removed.".postln;
				args.postln;
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
			data.do({
				| item, i |
				node = nodes[item];
				if( node.notNil, {
					node.moveTo(leftMargin@y);
					y = y + node.height + verticalSpacing;
					y.postln; 
				},{
					"Nil node encountered....".warn;
				})
			});
			
			y = y + verticalSpacing;
			this.bounds = this.bounds.height_(y);
			this.refresh();
			this.changed(this, \bounds);
		})
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//	Etc.
		
	bounds_{
		| rect |
		this.setProperty(\bounds, rect);
		dragLayer.bounds = rect.moveTo(0,0);
		contentLayer.bounds = rect.moveTo(0,0);
	}
	
	onClose {
		nodes = dragHandler = dragEvent = draggedItems = selectionManager = nil;
		this.releaseDependants();
	}
}