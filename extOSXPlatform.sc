+OSXPlatform {
	shutdown {
		this.loadShutdownFiles;
		HIDDeviceService.releaseDeviceList;
		if(Platform.ideName == "scapp"){
			CocoaMenuItem.clearCustomItems;
		};
	}
	
	shutdownFiles {
		var filename = "shutdown.rtf";
		^[this.systemAppSupportDir +/+ filename, this.userAppSupportDir +/+ filename];
	}

	
	loadShutdownFiles { 
		this.shutdownFiles.do{
			|afile|
			afile = afile.standardizePath;
			if(File.exists(afile), {afile.load})
		}
	}
}