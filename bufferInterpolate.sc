+SequenceableCollection {
	bufferInterpolate {
		| pos = 0, func |
		var adjusted, adjustedFloor, xfade;
		var oddSig, oddBuffer, evenSig, evenBuffer, sig;
		var oddFunc = (_ % 2).round(1);

		if (this.size > 1) 
		{
			adjusted = pos * (this.size - 1);
			adjustedFloor = adjusted.floor;
			
			xfade = adjusted - adjustedFloor;
			
			oddBuffer = Select.kr(
				oddFunc.(adjustedFloor).if(adjustedFloor, adjustedFloor + 1), 
				this);
			evenBuffer = Select.kr(
				oddFunc.(adjustedFloor).not.if(adjustedFloor, adjustedFloor + 1), 
				this);
			
			oddSig = func.value(oddBuffer);
			evenSig = func.value(evenBuffer);
			
			xfade = oddFunc.(adjustedFloor).if(1 - xfade, xfade);
			sig = (oddSig * xfade) + (evenSig * (1 - xfade));
		}
		{
			sig = func.(this[0]);
		};
		
		^sig;
	}
}