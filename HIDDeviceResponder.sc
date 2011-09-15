HIDDeviceResponder {
	classvar <>funcDict, <>isRunning = false, <>responderList;
	var <>func, deviceIndex;
	
	*new { arg func, deviceIndex = 0;
		^super.newCopyArgs(func, deviceIndex).init
	}
	
	init {
		if(funcDict.isNil, { 
			HIDDeviceService.buildDeviceList(nil, nil);
			responderList = List.new.add(this);
			funcDict = IdentityDictionary.new;
			funcDict.add(deviceIndex -> List.with(func));
		},{
			funcDict.at(deviceIndex).add(func);
			responderList.add(this);
		});
		HIDDeviceService.devices.at(deviceIndex).queueDevice;
		HIDDeviceService.action_({
			arg productID, vendorID, locID, cookie, val; 
			funcDict.at(deviceIndex).collect({ arg funk, i;
				funk.value(productID, vendorID, locID, cookie, val)
			})
		});	
		if (isRunning==false, { 
			HIDDeviceService.runEventLoop; 
			isRunning = true;
		});
	}
	
	*stopEventLoop {
		HIDDeviceService.stopEventLoop;
		isRunning = false;
	}
	
	*killAll {
		funcDict = nil;
		isRunning = false;
		HIDDeviceService.stopEventLoop;
		HIDDeviceService.releaseDeviceList;
		responderList = nil;
	}
	
	*getDeviceInfo {
		HIDDeviceService.buildDeviceList(nil, nil);
		HIDDeviceService.devices.do({arg dev;
			[dev.manufacturer, dev.product, dev.vendorID, dev.productID, dev.locID].postln;
			dev.elements.do({arg ele;
				[ele.type, ele.usage, ele.cookie, ele.min, ele.max].postln;
			});
		});
	}
	
	addFunc { arg func, deviceIndex = 0; 
		funcDict.at(deviceIndex).add(func);
	}
}
