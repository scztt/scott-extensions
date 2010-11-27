SCOutlineView : SCCompositeView {
	var <data, <rowData;
	var rows;
	var <>rowAction, 
		<>dataChangedAction, dataChangedActionWrapper, 
		<>selectionChangedAction, selectionChangedActionWrapper,
		<>rowDataChangedAction, rowDataChangedActionWrapper;
	var indentAmt = 8;
	var allowUpdate=true;
	var view, keyResponderView, <headerView, <footerView, <scrollView, <bodyView;
	var selection;
	
	*viewClass { ^SCCompositeView }
	
	*new {
		| view, bounds |
		^super.new(view,bounds).initOutline;
	}
	
	remove {
		this.removeAllRows();
		if( data.notNil, { data.removeDependant(dataChangedActionWrapper) });
		if( rowData.notNil, rowData.removeDependant(rowDataChangedActionWrapper));
		this.releaseDependants();
		^super.remove();
	}
	
	initOutline {
		rows = LinkedList.new;
		this.initKeyboardActions();
		this.canFocus = true;
		this.focus(true);
		keyResponderView = SCUserView(this, Rect(0,0,0,0))
			.focus(true)
			.focusColor_(Color.clear);
			
		headerView = SCCompositeView(this, Rect(0,0,this.bounds.width, 10));
		headerView.addFlowLayout(2@2,2@2);
		headerView.resize = 2;
		
		scrollView = SCScrollView(this, Rect(0,10,this.bounds.width,this.bounds.height-10))
			.resize_(5)
			.hasHorizontalScroller_(false);
		bodyView = SCCompositeView( scrollView, Rect(0,0,this.bounds.width,this.bounds.height-10))
			.resize_(5);
		bodyView.addFlowLayout(2@2,2@2);
		
		dataChangedActionWrapper = {
			|data|
			this.dataChangedAction.(this, data)
		};
		
		rowDataChangedActionWrapper = {
			|data|
			this.rowDataChangedAction.(this, data)
		};
		
		selectionChangedActionWrapper = {
			|selection|
			this.selectionChangedAction.(this, selection)
		};
		
		this.onClose = {
			if( data.notNil, { data.removeDependant(dataChangedActionWrapper) });
			if( rowData.notNil, rowData.removeDependant(rowDataChangedActionWrapper))
		}
	}
	
	data_{
		|inData|
		if(data.notNil, {
			data.removeDependant(dataChangedActionWrapper);
		});
		(data=inData).addDependant(dataChangedActionWrapper);
		dataChangedActionWrapper.(data);
	}
	
	rowData_{
		|inData|
		if(rowData.notNil, {
			rowData.removeDependant(rowDataChangedActionWrapper);
		});
		rowData = inData;
		rowData.addDependant(rowDataChangedActionWrapper);
		rowDataChangedActionWrapper.(rowData);
	}
	
	addRowAfter {
		| after, level=0, data, linkData=true |
		var row, prevLevel;
		prevLevel = after.notNil.if({after.level},{0});
		row = SCOutlineViewRow( bodyView, Rect(0,0,this.bounds.width,24), this, level )
			.resize_(2);
		if( (row.level > prevLevel) && after.notNil, {
			after.canExpand = true;
		});
		row.addDependant( this );
//		if( after.notNil, {
//			rows.put( rows.indexOf(after), row);
//		},{
			rows.add( row );
//		});
		row.data = data;
		rowAction.value( row, row.contentView );
		if( linkData.not, { row.data = nil });
		this.updateRowPositions();
	}
	
	addRow {
		| level, data, linkData=true |
		this.addRowAfter( rows.last, level, data, linkData );
	}
	
	updateRowPositions {
		var node, row, curExpansion=true, expansionLevel=0;
		var xIndent=3, yGap=2, yPos;
		yPos = 0 + headerView.bounds.height + 3;
		
		if( allowUpdate && (rows.size>0), {
			node = rows.nodeAt(0);
			row = node.obj;
			
			while({row.notNil}, {
				if( curExpansion.not && (row.level <= expansionLevel), {
					curExpansion = true;
				});
				
				if( curExpansion, {
					row.bounds = row.bounds.moveTo(xIndent, yPos);
					row.indent = row.level*indentAmt;
					yPos = yPos + row.bounds.height + yGap;
					
					curExpansion = row.expanded;
					expansionLevel = row.level;
					row.visible = true;				
				},{
					row.visible = false;
				});
								
				// Iterate
				node = node.next;
				node.notNil.if({
					row = node.obj;
				},{
					row = nil;
				})
			});
			bodyView.bounds = bodyView.bounds.height_(yPos);
		});
		
		this.refresh();
	}
	
	updateAfter {
		| func |
		allowUpdate=false;
		func.value;
		allowUpdate=true;
		this.updateRowPositions();
	}
	
	removeAllRows {
		rows.do({
			| row |
			row.releaseDependants();
			row.remove();
		});
		selection = nil;
		rows = LinkedList.new;
		this.updateRowPositions();
	}
		
	////////////////////////////////////////////////////////////
	// keyboard actions
	initKeyboardActions {
		parent.addAction({
			| view, character, modifiers, unicode, keycode |
			switch( keycode,
				// downarrow
				125, { this.selectNext() },
				
				// leftarrow
				123, { this.collapseSelectedRow() },
				
				// rightarrow
				124, { this.expandSelectedRow() },
				
				// uparrow
				126, { this.selectPrev() }
			);
			
		}, \keyUpAction);
	}	
	
	
	selectNext {
		var node;
		selection.notNil.if({
			node = rows.findNodeOfObj(selection);
			(node.notNil and: {node.next.notNil}).if({ 
				this.select(node.next.obj)
			},{
				this.select(nil)
			})
		},{
			this.select( rows.first );
		})
	}
	
	selectPrev {
		var node;
		selection.notNil.if({
			node = rows.findNodeOfObj(selection);
			(node.notNil and: {node.prev.notNil}).if({
				this.select(node.prev.obj);
			},{
				this.select(nil);
			})
			
		},{
			this.select( rows.last );
		})
	}
	
	select {
		| newSelection |
		var oldSelection = selection;
		(selection != newSelection).if({
			selection = newSelection;
	
			oldSelection.notNil.if({ 
				oldSelection.showSelected = false;
				oldSelection.data.removeDependant(rowDataChangedActionWrapper);
				this.rowData = nil;
			});
			selection.notNil.if({
				selection.showSelected = true;
				selectionChangedAction.(newSelection);
				this.rowData = selection.data;
			});			
		});
	}
	
	expandSelectedRow {
		selection.notNil.if({ selection.expanded = true });
	}
	
	collapseSelectedRow {
		selection.notNil.if({ selection.expanded = false });
	}
	
	update {
		| obj, msg |
		if( obj==data, {
			this.onDataChanged( msg );
		},{
			switch( msg, 
				\expanded, {
					this.updateRowPositions();
				},
				\rowClicked, {
					this.select( obj )
				}
			)
		});
	}
}

SCOutlineViewRow : SCCompositeView {
	var canExpand=false, <expanded=true, indent=0, button, <buttonView, <contentView;
	var <>level, <>parent, showSelected, mouseResponder, <data;
	var <>dataChangedAction, dataChangedActionWrapper, originalBackground;
	
	*viewClass { ^SCCompositeView }

	*new {
		| view, bounds, aParent, aLevel=0, aCanExpand=false, aExpanded=true, aIndent=0 |
		^super.new(view, bounds).initRow( aParent, aLevel, aCanExpand, aExpanded, aIndent );
	}
	
	data_{
		| inData |
		if( data.notNil, { data.removeDependant(dataChangedActionWrapper) });
		data = inData;
		data.addDependant(dataChangedActionWrapper);
		dataChangedActionWrapper.(data);
	}
	
	background_{
		| color |
		originalBackground = color;
		background = color;
		this.setProperty(\background, color);
	}
	
	remove {
		data.notNil.if({ 
			data.removeDependant(dataChangedActionWrapper) 
		});
		this.releaseDependants();
		^super.remove();
	}
	
	initRow {
		| aParent, aLevel, aCanExpand, aExpanded, aIndent |
		parent = aParent;
		level = aLevel;
		expanded = aExpanded;
		indent = aIndent;
		mouseResponder = SCUserView( this, this.bounds.moveTo(0,0))
			.focusColor_(Color.clear);
		contentView = SCCompositeView(this, Rect(indent,0,this.bounds.width-20,this.bounds.height));
		contentView.addFlowLayout(3@3, 3@1);
		contentView.resize_(4);
		buttonView = SCCompositeView(contentView, Rect(0,0,14+indent,20));
		
		this.canExpand = aCanExpand;
				
		mouseResponder.mouseUpAction = {
			this.changed( \rowClicked )
		};
		
		mouseResponder.keyUpAction = {
			
		};
		
		dataChangedActionWrapper = {
			|...args|
			this.dataChangedAction.(this, *args);
		};
		
		this.onClose = {
			if(data.notNil, {
				data.removeDependant(dataChangedActionWrapper);
			})
		};
	}
	
	onClick {
		this.changed( \rowClicked );
		this.refresh();
	}
	
	canExpand_{
		| val |
		canExpand=val;
		if( canExpand && (button.isNil), {
			button = RoundButton( buttonView, Rect(1,1,14,14) )
				.radius_(0).border_(0)
				.states_([[\play], [\down]])
				.action_({
					| button |
					this.expanded = expanded.not;
				})
				.value_( expanded.if({1},{0}) )
				.canFocus_(false);
		});		
	}
	
	indent_{
		|val|
		indent = val;
		contentView.bounds = Rect(indent,0,this.bounds.width-20,this.bounds.height);
	}
	
	expanded_{
		| exp |
		if( canExpand, {
			expanded = exp;
			button.value = exp.if({1},{0});
			this.changed( \expanded );
		});
	}
	
	showSelected_{
		| val |
		showSelected = val;
		if( showSelected, {
			this.setProperty(\background, Color.blue(0.7, 0.2));
		},{
			this.setProperty(\background, originalBackground ? Color.clear);
		})
	}	
}
