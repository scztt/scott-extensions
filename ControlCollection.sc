ControlCollection {
	var envir, <controls, <states, <currentState;
	var parentPage, page, <>fadeTime=1, updating=false;
	var stateSwitcher, textBox;
	
	*new {
		| envir, controls, states |
		^super.newCopyArgs( envir, controls, states).init;
	}
	
	init {
		var controlDict;
		controls = controls ? envir.nonNodes.select({ | elem, key | elem.class == ControlNode }).keys.asList;
		states = states ? Dictionary.new;
		this.loadPresets;
		^this;
	}
	
	add {
		| control |
		controls.add( control );
	}
	
	remove {
		| control |
		controls.remove( control );
	}

	setState {
		| state |
		var msg, cont;
		msg = [];
		if( states.keys.includes( state ), {
			currentState = state;
			states[currentState].keysValuesDo({
				| key, value, num |
				cont = envir.at( key );
				cont.isNil.not.if({
					msg = msg ++ cont.setMsg( value, fadeTime );
				})
			})
		},{"no such state".postln});
		envir.server.listSendBundle( nil, msg );
		^currentState
	}
	
	saveState {
		| symbol |
		var newState;
		newState = Dictionary.new;
		states.put( symbol, newState );
		controls.do({
			| key |
			envir[key].get({ 
				| value |
				newState.put( key, value );
			})
		})
	}
	
	randomize {
		controls.do({
			| key |
			envir[key].randomize( fadeTime );
		})
	}
	
	updateAll {
		controls.do({
			| key |
			envir[key].update;
		})
	}
	
	makeControlPanel {
		| page |
		var updateButton, saveButton, fadeTimeText, fadeTimeBox;
		parentPage = page;
		parentPage.startRow;
		page = FlowView( page, page.layRight( 800, 25 ) );
		
		updateButton = ActionButton( page, "updating", {
			updating = updating.not;
			controls.do({
				|key|
				envir[key].update( updating );
			});
			updating.if({
				updateButton.labelColor_(Color.black);
			},{
				updateButton.labelColor_(Color.grey );
			});
		}, minHeight:20, color:Color.grey);
		textBox = SCTextField( page, page.layRight( 80, 20 ) )
			.string_( "" )
			.mouseDownAction_({
				| box | box.string_("");
			})
			.action_({
				if( textBox.string.size>0, {
					this.saveState( textBox.string.asSymbol );
					stateSwitcher.value_( stateSwitcher.items.indexOf(textBox.string.asSymbol) );
					stateSwitcher.items_( states.keys.asArray );
					textBox.string_("");
				})
			});
		saveButton = ActionButton( page, "save", {
			if( textBox.string.size>0, {
				this.saveState( textBox.string.asSymbol );
				stateSwitcher.items_( states.keys.asArray );
				stateSwitcher.value_( stateSwitcher.items.indexOf(textBox.string.asSymbol) );
				textBox.string_("");
			})
		}, minHeight:20 );
		stateSwitcher = SCPopUpMenu( page, page.layRight( 80, 20 ) )
			.items_( states.keys.asArray )
			.action_({ | popup |
				popup.items[ popup.value ].postln;
				this.setState( popup.items[popup.value].asSymbol );
				textBox.string_( popup.items[popup.value] );
			});
		fadeTimeBox = SCNumberBox( page, page.layRight( 40, 20 ) )
			.value_( fadeTime )
			.action_({ |box| fadeTime = box.value });
		parentPage.reflowAll.resizeToFit;
	}
	
	makeAllControls {
		| page |
		page = page ? MultiPageLayout( envir.name ++ " controls", Rect( 100, 100, 500, 700 ) );
		page.window.onClose_({ this.onClose});
		this.makeControlPanel( page.view );
		controls.do({
			|key|
			var item;
			item = envir[key];
			if( item.class == ControlNode , {
				item.makeControlPanel( page.view );
			})
		});
		page.view.reflowAll;
		page.resizeToFit.front;
		^page
	}
	
	savePresets {
		| name |
		var path;
		this.saveState( "default" );
		{
			name = name ? envir.name;
			path = Platform.userAppSupportDir +/+ "presets" +/+ name ++ ".preset";
			path = path.standardizePath;
			states.writeTextArchive( path );
			(states.keys.size.asString ++ " states saved to " ++ path).postln;
		}.defer(0.2);
	}

	loadPresets {
		| name, initialPreset="default" |
		var path;
		name = name ? envir.name;
		path = Platform.userAppSupportDir +/+ "presets" +/+ name ++ ".preset";
		path = path.standardizePath;
		states = Dictionary.readTextArchive( path ) ? Dictionary.new;
		states.isNil.not.if({
			this.setState( initialPreset );
			stateSwitcher.isNil.not.if({
				stateSwitcher.items_( states.keys.asArray );
				stateSwitcher.value_( stateSwitcher.items.indexOf( initialPreset ) ? 0 );
			});
			textBox.isNil.not.if({
				textBox.string_( currentState.asString );
			});
		});
	}
	
	onClose {
		this.savePresets;
	}

	resizeWindow {
		var bounds = parentPage.bounds;
		bounds.resizeBy(10,10);
		page.window.bounds = bounds.moveTo(page.bounds.left, page.bounds.top - (bounds.height-page.window.bounds.height));
	}
}