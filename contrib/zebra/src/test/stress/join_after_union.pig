register $zebraJar;
--fs -rmr $outputDir


a1 = LOAD '$inputDir/25Munsorted1' USING org.apache.hadoop.zebra.pig.TableLoader('count,seed,int1,str2,long1');
a2 = LOAD '$inputDir/25Munsorted2' USING org.apache.hadoop.zebra.pig.TableLoader('count,seed,int1,str2,long1');    
a3 = LOAD '$inputDir/25Munsorted3' USING org.apache.hadoop.zebra.pig.TableLoader('count,seed,int1,str2,long1');
a4 = LOAD '$inputDir/25Munsorted4' USING org.apache.hadoop.zebra.pig.TableLoader('count,seed,int1,str2,long1');

sort1 = order a1 by long1;
sort2 = order a2 by long1;  
sort3 = order a3 by long1;
sort4 = order a4 by long1;

store sort1 into '$outputDir/25Msorted1' using org.apache.hadoop.zebra.pig.TableStorer('[count,seed,int1,str2,long1]');
store sort2 into '$outputDir/25Msorted2' using org.apache.hadoop.zebra.pig.TableStorer('[count,seed,int1,str2,long1]'); 
store sort3 into '$outputDir/25Msorted3' using org.apache.hadoop.zebra.pig.TableStorer('[count,seed,int1,str2,long1]');
store sort4 into '$outputDir/25Msorted4' using org.apache.hadoop.zebra.pig.TableStorer('[count,seed,int1,str2,long1]');     
                                                                                                                        

joinl = LOAD '$outputDir/25Msorted1,$outputDir/25Msorted2' USING org.apache.hadoop.zebra.pig.TableLoader('count,seed,int1,str2,long1', 'sorted');
joinll = order joinl by long1; 
store joinll into '$outputDir/unionl' using org.apache.hadoop.zebra.pig.TableStorer('[count,seed,int1,str2,long1]');          


joinr = LOAD '$outputDir/25Msorted3,$outputDir/25Msorted4' USING org.apache.hadoop.zebra.pig.TableLoader('count,seed,int1,str2,long1', 'sorted');
joinrr = order joinr by long1; 
store joinrr into '$outputDir/unionr' using org.apache.hadoop.zebra.pig.TableStorer('[count,seed,int1,str2,long1]');


rec1 = load '$outputDir/unionl' using org.apache.hadoop.zebra.pig.TableLoader();
rec2 = load '$outputDir/unionr' using org.apache.hadoop.zebra.pig.TableLoader();   


joina = join rec1 by long1, rec2 by long1 using "merge" ;

E = foreach joina  generate $0 as count,  $1 as seed,  $2 as int1,  $3 as str2, $4 as long1;

store E into '$outputDir/join_after_union_1' using org.apache.hadoop.zebra.pig.TableStorer('');                  
