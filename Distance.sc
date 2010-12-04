// Distance calculation taken from:
//	http://www.sengpielaudio.com/calculator-air.htm
// which in turn comes from ISO 9613-1:1993

DistAttenuate : UGen {
	*ar {
		| sig, distance=1, temp=25, relHumidity=70, minDist=1, maxDist=9999, maxDelay=1 |
		var distAtten, distFilter, filteredSig,
			baseFilterFreq=20000; 
		distAtten = 20*(0.43429*log(minDist/min(distance,maxDist)));
		distFilter = distance * this.distanceFiltering( baseFilterFreq, temp, relHumidity );
		filteredSig = BPeakEQ.ar( sig, baseFilterFreq, 8, distFilter ) * distAtten.dbamp;
		filteredSig = DelayC.ar( filteredSig, maxDelay, distance/343.0 );
		^filteredSig
	}

	*distanceFiltering {
		| freq=440, temp=25, relHumidity=70|
		var c_humid, db_humid, db_humid_ft, db_humi, pres, humidity, tempr, alpha,
		frO, frN;
		pres = 101325;
		
		temp = temp + 273.15;
		pres = pres / 101325.0;
	
		c_humid = 4.6151 - (6.8346 * pow((273.15 / temp), 1.261));
		humidity = relHumidity * pow(10, c_humid) * pres;
		tempr = temp / 293.15;
		frO = pres * (24 + (4.04e4 * humidity * (0.02 + humidity) / (0.391 + humidity)));
		frN = pres * pow(tempr, -0.5) * (9 + (280 * humidity * exp( - 4.17 * (pow(tempr, -1/3) - 1))));
	
		alpha = 	8.686 * 
				freq * 
				freq * 
				( 
					1.84e-11 * 
					(1 / pres) * 
					sqrt(tempr) + 
					( 
						pow(tempr, -2.5) *
						(
							0.01275 * 
							(
								exp(- 2239.1 / temp) *
								1 / 
								(
									frO + 
									(
										freq * 
										freq / 
										frO
									)
								)
							) + 
							(
								0.1068 * 
								(
									exp( - 3352 / temp) * 
									1 / 
									(frN + freq * freq / frN)
								)
							)
						)
					)
				);
		db_humid = 100 * alpha;
		if (db_humid < 0.0001, {db_humid = 0});
		db_humid_ft = 0.3048 * db_humid;
		db_humi = alpha;
		^db_humi.neg
	}
}



//BPeakEQ.magResponse( (100..20000), 44100, 20000, 9.0, ~distance.(20000, 20, 15)).ampdb.plot2;