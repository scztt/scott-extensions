+Number {
	diffCircle {
		| a, shortest=true |
		var b;
		b = this.mod(pi*2);
		a = a.mod(pi*2);
		if( shortest, {
			^if( (a-b).abs < pi, {
				(a-b)
			},{
				(a-b+(pi*2*(a-b).sign.neg));
			})			
		},{
			^if( (a-b).abs > pi, {
				(a-b)
			},{
				(a-b+(pi*2*(a-b).sign.neg));
			})			

		})
	}
}