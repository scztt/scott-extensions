SynthDefHistoryWindow {
	classvar <classPath;
	var window, <outline, footer;
	var footerData, footerSynth, footerName, footerDesc, footerComments, footerDate, footerStar, footerSynthDef;
	var selectAction, selection;
	var dataToRowMap;
	var synthDefProto;
	var iconPath;
	
	*new {
		^super.new.init();
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//
	init {
		iconPath = PathName(this.class.filenameSymbol.asString).pathOnly;
		window = SCWindow("history", Rect(300,300,510,500));
		outline = SCOutlineView(window, Rect(0,0,510,500-200))
			.resize_(5);
		this.initFooter();
		outline.dataChangedAction = { |view| this.onDataChanged(view) };
		outline.rowDataChangedAction = { |view, data| this.onRowDataChanged(view, data) };
		outline.rowAction = { |row, contentView| this.onRowAdded(row, contentView) };
		outline.data = SynthDefHistory;
		
		synthDefProto = "SynthDef.newTrack(%, %).add;";
		
		window.front;
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//
	initFooter {
		footer = SCCompositeView( window, Rect(0,window.bounds.height-200,window.bounds.width, 200))
			.background_(Color.grey(1,0.1))
			.resize_(8);
		footer.addFlowLayout(8@8, 2@2);
		footerStar = RoundButton( footer, 18@18 )
			.radius_(0).border_(0).canFocus_(false)
			.action_({ |view| })
			.states_([
				[SCImage(iconPath +/+ "star_silver.png")],
				[SCImage(iconPath +/+ "star.png")]
			])
			.action_({
				| view |
				outline.rowData.notNil.if({
					outline.rowData[\rating] = view.value;
					outline.rowData.changed();
				})
			});
		footerSynth = SCStaticText( footer, 150@18)
			.resize_(8)
			.string_("")
			.font_(Font("Helvetica", 14).boldVariant);			
		footer.decorator.nextLine;
		footerDesc = SCTextView( footer, (footer.bounds.width-16) @18 )
			.resize_(8)
			.background_(Color.grey(0.9))
			.string_("")
			.focusColor_(Color.clear)
			.keyUpAction_(Collapse({
				| view |
				outline.rowData.notNil.if({
					outline.rowData[\description] = view.string;
					outline.rowData.changed();
				})
			}, 0.2));
			
		footer.decorator.nextLine.shift(1);
		footerComments = SCTextView( footer, (footer.bounds.width-16) @40 )
			.resize_(8)
			.background_(Color.grey(0.9))
			.font_(Font("Helvetica", 11))
			.string_("")
			.hasVerticalScroller_(true)
			.focusColor_(Color.clear)
			.keyUpAction_(Collapse({
				| view |
				outline.rowData.notNil.if({
					outline.rowData[\comments] = view.string;
					outline.rowData.changed();
				})
			}, 0.2));

		footer.decorator.nextLine.shift(1);
		footerSynthDef = SCTextView( footer, (footer.bounds.width-16) @80 )
			.resize_(8)
			.background_(Color.grey(0.9))
			.font_(Font("AndaleMono", 10))
			.string_("")
			.editable_(false)
			.hasVerticalScroller_(true)
			.focusColor_(Color.clear);
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//
	onRowAdded {
		| row, contentView |
		var displayString, lastClick=0, desc, descEdit;
		var star, symbol;
		switch( row.level, 
			0, {
				symbol = row.data;
				SCStaticText( contentView, 300@18 )
					.string_("\\"++symbol)
					.font_(Font("Helvetica", 12).boldVariant);
				row.background_(Color.grey(0.1,0.1));
	
				contentView.decorator.left = contentView.decorator.left + 10;

				// print button
				RoundButton( contentView, 18@18 )
					.radius_(0).border_(0).canFocus_(false)
					.mouseDownAction_({ row.onClick(); })
					.action_({ 
						| view |
						var string="";
						outline.data.archiveAt(symbol).do({
							| item |
							string = string ++ "/////////////////////////////////////////////////////////////////////////////////////////\n";
							string = string ++ "// date: %\n".format(item[\date]);
							if (item[\description].size>0, {
								string = string ++ "// description: %\n".format(item[\description])
							});
							if (item[\comments].size>0, {
								item[\comments].split(Char.nl).do({
									| line |
									string = string ++ "//\t%\n".format(line)
								})
							});
							string = string ++ synthDefProto.format(
									"\\" ++ item[\name], item[\funcString]);
							string = string ++ "\n\n\n"
						});
						Document.new("\\" ++ symbol, string).syntaxColorize;
					})
					.states_([[SCImage(iconPath +/+ "printer.png")]]);
				
				// clean button
				RoundButton( contentView, 18@18 )
					.radius_(0).border_(0).canFocus_(false)
					.mouseDownAction_({ row.onClick(); })
					.action_({ 
						| view |
						var arch, keep;
						arch = outline.data.archiveAt(symbol);
						arch.notNil.if({
							outline.updateAfter({
								keep = arch.selectAs({
									|item|
									item[\rating].notNil.and({item[\rating]>0});
								}, List);
								arch.clear.addAll(keep);
								{ outline.data.changed() }.defer(0.00001);
							})
						})
					})
					.states_([[SCImage(iconPath +/+ "bin.png")]]);
			},
			1, {
				row.dataChangedAction = {
					| view, data |
					if (descEdit.string != data[\description], { 
						descEdit.string = data[\description] ? data[\date].asString ? "" ;
					});
					data[\description].isNil.if({
						descEdit
							.font_(Font("Helvetica-Oblique", 12))
							.stringColor_(Color.grey(0.4))
					},{
						descEdit
							.font_(Font("Helvetica", 12).boldVariant)
							.stringColor_(Color.black)
					});					
					if (star.value != data[\rating],	
						{ star.value = data[\rating] });
				};
				displayString = row.data[\description] ? row.data[\date] ? "";
//				SCDragSource( contentView, 18@18)
//					.beginDragAction_({   synthDefProto.format("\\" ++ row.data[\name], row.data[\funcString]); })
//					.string_("").object_("");
					
				descEdit = SCTextField( contentView, 220@18 )
					.resize_(2)
					.background_(Color.grey(1,0.1))
					.string_(displayString)
					.font_( row.data[\description].isNil.if(
						{Font("Helvetica-Oblique", 12)}, 
						{Font("Helvetica", 12).boldVariant}
					))
					.stringColor_( row.data[\description].isNil.if(
						{ Color.grey(0.4) },
						{ Color.black }
					))
					.focusColor_(Color.clear)
					.mouseDownAction_({ 
						row.onClick();
					})
					.action_({
						|view|
						row.data[\description] = view.string.asString;
						row.data.changed();
					});
				
				SCStaticText( contentView, 130@18 )
					.resize_(3)
					.font_(Font("HelveticaNeue-LightItalic",10))
					.string_(row.data[\date].asString);
	
				// star button
				star = RoundButton( contentView, 18@18 )
					.resize_(3)
					.radius_(0).border_(0).canFocus_(false)
					.action_({ 
						| view |
	 					row.data[\rating] = view.value;
						row.data.changed();
						row.onClick();
					})
					.states_([
						[SCImage(iconPath +/+ "star_silver.png")],
						[SCImage(iconPath +/+ "star.png")]
					])
					.value_(row.data[\rating] ? 0);
					
				// paste button
				RoundButton( contentView, 18@18 )
					.resize_(3)
					.radius_(0).border_(0).canFocus_(false)
					.mouseDownAction_({ row.onClick() })
					.mouseUpAction_({ 
						| view, key, code, mod |
						var range, string;
						if( mod>256,
						{
							string = synthDefProto.format(
								"\\" ++ row.data[\name], row.data[\funcString]);
						},{
							string = row.data[\funcString].findRegexp("(?ism)\\{(.*)\\}")[1][1];
						});
						range = [ Document.current.selectionStart, string.size];
						Document.current.selectedString_(string);
						Document.current.selectRange(*range);
					})
					.states_([[SCImage(iconPath +/+ "paste_plain.png")]]);
	
				// send button
				RoundButton( contentView, 18@18 )
					.resize_(3)
					.radius_(0).border_(0).canFocus_(false)
					.mouseDownAction_({ row.onClick(); })
					.action_({ 
						| view |
						SynthDef(row.data[\name], row.data[\func]).add;
					})
					.states_([[SCImage(iconPath +/+ "lightning_add.png")]]);
				contentView.decorator.left = contentView.decorator.left + 8;
	
				// delete button
				RoundButton( contentView, 18@18 )
					.resize_(3)
					.radius_(0).border_(0).canFocus_(false)
					.mouseDownAction_({ row.onClick(); })
					.action_({ 
						| view |
						{ SynthDefHistory.delete(row.data[\name], row.data) }.defer(0.000001);
					})
					.states_([[SCImage(iconPath +/+ "delete.png")]]);
			}
		);
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//
	onDataChanged {
		| view |
		var data = view.data;
		// just update all for now.
		view.updateAfter({
			view.removeAllRows();
			
			SynthDefHistory.archives.keysValuesDo({
				| key, arch |
				view.addRow( 0, key, false );
				arch.do({
					| def |
					view.addRow( 1, def );
				})
			})		
		})
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//
	onRowDataChanged {
		|view, data|
		if( data.notNil, {
			footerSynth.string = "\\" ++ (data[\name].asString ? "");
			if (footerDesc.string != data[\description] ? data[\date].asString, { 
				footerDesc.string = data[\description] ? data[\date].asString ? "" ;
			});
			
			data[\description].isNil.if({
				footerDesc
					.font_(Font("Helvetica-Oblique", 12))
					.stringColor_(Color.grey(0.4))
			},{
				footerDesc
					.font_(Font("Helvetica", 12).boldVariant)
					.stringColor_(Color.black)
			});
			
			if (footerComments.string != data[\comments],
				{ footerComments.string = data[\comments] ? "" });
				
			if (footerStar.value != data[\rating],	
				{ footerStar.value = (data[\rating] ? 0) });
				
			footerSynthDef.string = data[\funcString] ? "";
		},{
			footerSynthDef.string = "";
			footerDesc.string = "";
			footerComments.string = "";
			footerSynthDef.string = "";
			footerStar.value = 0;
		})	
	}
	
}
