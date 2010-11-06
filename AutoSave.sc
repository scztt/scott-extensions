AutoSave {
	classvar saving = false, <>verbose=false;
	classvar <>autosavePath, routine, <>asDocList, <>interval=1;
	classvar <>appLaunched=false;

	*classinit {
		autosavePath = Platform.userAppSupportDir ++ "/autosave/";
	}
	
	*on {
		| interval=1 |
		this.interval = interval;
		if( saving.not, {
			"Autosaving.".postln;
			asDocList = Set();
			saving = true;
			routine = Routine({
				loop {
					AutoSave.autosaveAction;
					(this.interval*60).yield;
				}
			}).play(AppClock);
			CmdPeriod.add( this );
		});
		appLaunched = true;
	}

	*off {
		if( saving, {
			saving = false;
			routine.stop.reset;
			routine = nil;
			asDocList = List();
		});
		CmdPeriod.remove( this );
	}
	
	*autosaveAction {
		var outFile, count=0;
		Document.allDocuments.do({
			| doc |
			var path;
			if( (doc.path.isNil or: {doc.path.containsi("/Help/").not }) && doc.isEdited, {
				path = autosavePath ++ doc.name;
				asDocList.add( path );
				outFile = File( path, "w" );
				outFile.write( doc.string );
				outFile.close;
				count = count+1;
				
				doc.onClose.value;
				doc.onClose_({ File.delete( path ); asDocList.remove(path) });
			})
		});
	
		if( verbose && (count>0), { ("Autosaved " ++ count ++ " documents.").postln });
	}
	
	*clean {
		("rm " ++  AutoSave.autosavePath.escapeChar($ ) ++ "*").unixCmd;
		asDocList = List();
	}
	
	*restore {
		| filename |
		var path;
		if( (filename.class == PathName).not, {
			path = PathName( autosavePath ++ filename );
		},{ path = filename });
		if( path.isFile, {
			Document.new( path.fileNameWithoutExtension ++ "-autosaved." ++ path.extension, 
				File( path.asAbsolutePath, "r" ).readAllString );
		})
	}
	
	*remove {
		| filename |
		var path;
		if( (filename.class == PathName).not, {
			path = PathName( autosavePath ++ filename );
		},{ path = filename });
		if( path.isFile, {
			File.delete( path.asAbsolutePath );
			 
		})
	}
	
	*restoreWindow {
		var fileList;
		var window, list, restoreBut, deleteBut;
		fileList = PathName( autosavePath ).entries;
		fileList = fileList.removeAllSuchThat({
			|path|
			Document.allDocuments.collect( _.name )
				.find( [path.fileName] ).isNil;
		});
		if( fileList.size>0, {
			window = SCWindow( "Restore autosaved files", 
				Rect( 200,200, 210, 65 + (18*fileList.size)) );
			SCStaticText( window, Rect( 5, 5, 200, 30 ) )
				.string_( "The following autosaved files were found:")
				.font_( Font( "Helvetica", 10 ) );
			list = SCListView( window, Rect(5, 30, 200, 18*fileList.size ) )
				.items_( fileList.collect( _.fileName ) )
				.font_( Font( "Helvetica", 10 ) )
				.resize_( 5 );
			restoreBut = SCButton( window, Rect( 5, list.bounds.bottom+5, 60, 20 ))
				.states_([[ "Restore", Color.black, Color.clear ]])
				.action_({ |v| this.restore( fileList[list.value] ) })
				.resize_( 7 );
			deleteBut = SCButton( window, Rect( 70, list.bounds.bottom+5, 60, 20 ))
				.states_([[ "Delete", Color.black, Color.clear ]])
				.action_({ 
					|v| 
					this.remove( fileList[list.value] );
					fileList = PathName( autosavePath ).entries;
					fileList = fileList.removeAllSuchThat({
						|path|
						Document.allDocuments.collect( _.name )
							.find( [path.fileName] ).isNil;
					});
					list.items_( fileList.collect( _.fileName ) )
			 	})
			 	.resize_( 7 );
			
			window.front;
		});
	}
	
	*cmdPeriod {
		if( routine.notNil, {
			{ routine.play( AppClock ) }.defer( this.interval*60 );
		});
	}
}