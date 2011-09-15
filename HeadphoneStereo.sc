HeadphoneStereo {
	*ar {
		arg in, x, y;
		var distR, distL, leftSig, rightSig;

		distR = ( ((0.05-x)**2) + ((0-y)**2) )**0.5;
		distL = ( ((x-0.05)**2) + ((0-y)**2) )**0.5;
		rightSig = Distance.norevdop( in, distR );
		leftSig = Distance.norevdop( in, distL );
		^[rightSig, leftSig];
	}
}