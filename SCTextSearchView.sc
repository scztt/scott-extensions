SCTextSearchView {
	classvar <>classlist;
 	var <textbox, string, listPosition=0, listDisplayPosition=nil, numChars=0, currentChar;
 	var positionList;
 	var update = false;
 	var <searchList, <>action, <>escAction, autosort;

	*new {
		| window, rect, sort=true |
		^super.new.init( window, rect, sort )
	}
	
	*newHelpWindow {
		var w, tsv, mouse;
		classlist = if( classlist.isNil,{ 
			Object.allSubclasses.collect( _.asString ).sort({|a,b| a<b }) 
		},{ classlist });
		mouse = Platform.getMouseCoords();
		w = GUI.current.window.new( "Search help for..." , 
			Rect( mouse.x, mouse.y.min(SCWindow.screenBounds.height-100), 300, 30 ), border:false);
		tsv = this.new( w, Rect(5,5,300-10,30-10), false)
			.action_({ |str| str.openHelpFile; w.close })
			.escAction_({ w.close })
			.searchList_( classlist );
		tsv.textbox.focus(true).font_(Font( "Helvetica", 16));
		w.front;
		^tsv
	}

	*newExecWindow {
		var cmds, w, tsv, mouse, exception;
		cmds = Archive.global.at(\ExecWindowRecentCommands) ? Array.newFrom(""!40);
		cmds = cmds.sort({|a,b| a<b });
		mouse = Platform.getMouseCoords();
		w = GUI.current.window.new( "Exec command..." , 
			Rect( mouse.x, mouse.y.min(SCWindow.screenBounds.height-100), 300, 30 ), border:false);
		tsv = this.new( w, Rect(5,5,300-10,30-10), false)
			.action_({
				|str| 
				{ 
					str.interpret();
					cmds = cmds.rotate(1).put(0,str);
					Archive.global.put(\ExecWindowRecentCommands, cmds);
				}.try({
					| e |
					exception = e;
				});
				w.close();
				exception.notNil.if({ exception.throw() });
			})
			.escAction_({ w.close })
			.searchList_( cmds );
		tsv.textbox.focus(true).font_(Font( "Helvetica", 16));
		w.front;
		^tsv
	}

	init {
		| window, rect, sort |
		textbox = GUI.current.textView.new( window, rect )
			.background_(Color.grey(0.9))
			.keyDownAction_({ |a,b,c,d| this.keyDown(a,b,c,d) })
			.keyUpAction_({ |a,b,c,d| this.keyUp(a,b,c,d) });
		string = "";
		searchList = [];
		positionList = Array.newClear(50);
		positionList[0] = 0;
		autosort = sort;
	}
		
	searchList_{
		| list |
		if( autosort, {
			searchList = list.sort({|a,b| a<b});
		},{
			searchList = list;
		})
	}
	
	keyDown {
		| a,b,c,d |
		if( (d>=14) && (d<=126), {
			// keystroke
			string = string ++ d.asAscii;
			currentChar = d.asAscii;
			numChars = string.size;
			this.doSearch;
			textbox.string_(string[0..string.size-2]);
			update = true;
		});
		if( d==9, {
			// tab
			this.doNext;
			update = true;
		});
		if( d==127, {
			// backspace
			if( string.size>1, {
				string = string.copyRange(0,string.size-2);
				currentChar = string.last.asAscii;
				
				positionList[numChars] = nil;
				numChars = string.size;
				listPosition = listDisplayPosition = positionList[numChars];
			},{
				positionList[1] = nil;
				listDisplayPosition = nil;
				this.clear;
			});
			update = true;
		});
	}
	
 	keyUp {
		| a,b,c,d |
		if( d==13, {
			//enter
			if( listDisplayPosition.notNil, {
				action.value( searchList[listDisplayPosition] );
			},{
				action.value( string );
				this.clear;
			})
		});
		if( d==27, {
			escAction.value;
		});
		if( update, {
			this.updateText();
		});
	}
	
	clear {
		string = "";
		listPosition = 0;
		currentChar = nil;
		numChars = 0;
		update = true;
	}
	
	doSearch {
		var n;
		if( (numChars>0) && (searchList.size>0) && listPosition.notNil, {
			n = listPosition;
			listPosition = listDisplayPosition = nil;
			while({ (n < searchList.size) && listPosition.isNil}, {
				if( this.beginsWith( string, searchList[n] ), {
					listPosition = n;
					listDisplayPosition = n;
					positionList[numChars] = n;
				});
				n = n+1;
			})
		})
	}
		
	doNext {
		if( listDisplayPosition.notNil, { 
			if( (listDisplayPosition+1) < searchList.size, {
				listDisplayPosition = listDisplayPosition+1;
				if( this.beginsWith( string, searchList[ listDisplayPosition ] ).not, {
					listDisplayPosition = listPosition;
				})
			}) 
		})
	}
	
	beginsWith {
		| partial, full |
		^( partial.compare( full[0..partial.size-1], true) == 0 )
	}
	
	updateText {
		if( listDisplayPosition.notNil, {
			textbox.setStringColor( Color.black, 0, 99 );
			textbox.string_( searchList[ listDisplayPosition ] );
			textbox.setStringColor( Color.grey(0.4), string.size, 99 );
		},{
			textbox.string_( string );
		});
		update = false;
	}
 }