-- %declare date '20080228'
-- l = load '$xyz';

/* comment */ 

/* l = load '$zxv';
   store l into '$zxcv';
*/
aa = load '/data/intermediate/pow/elcarobootstrap/account/full/weekly/data/20080228' using PigStorage('\x01');
bb = filter aa by (ARITY == '16') and ( $4 eq '' or $4 eq 'NULL' or $4 eq 'ss') parallel 400;
a = foreach bb generate /* $col 1 */ $0,$12,$7;  
/* $date /* */
--generate inactive accts
inactiveAccounts = filter a by ($1 neq '') and ($1 == '2') parallel 400; -- testing $comment
store inactiveAccounts into '/user/kaleidoscope/pow_stats/20080228/acct/InactiveAcct';
grpInactiveAcct = group inactiveAccounts all;
countInactiveAcct = foreach grpInactiveAcct { generate COUNT( inactiveAccounts ); }
store countInactiveAcct into '/user/kaleidoscope/pow_stats/20080228/acct_stats/InactiveAcctCount';
