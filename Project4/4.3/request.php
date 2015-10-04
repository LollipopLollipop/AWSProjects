<?php
if ( !isset($_REQUEST['term']) )
{       echo 'error';
        exit;
}
//echo 'hello';
include('./httpful.phar');
//echo 'again';
// EDIT these to match your schema
$tableName="languagemodelgene";
$columnFamily="cf";

// trim leading and trailing spaces
$searchTerm = strtolower(trim( $_REQUEST['term'] ));

// Connect to HBASE server
//  note: we are running in non "security" mode, so no user auth needed
$url = "http://localhost:8080/".$tableName."/".urlencode($searchTerm);

//Send Request
$response = \Httpful\Request::get($url)->addHeader('Accept','application/json')->send();

//DEBUG
//echo $response;

// iterate through response, adding each to array
$data = array();
$json = json_decode($response,true);
$row=$json["Row"];
$Cell=$row["Cell"];
$line=base64_decode($Cell["$"]);
$line=rtrim($line, ";");//remove the tailing ; of each string 
//echo $line;
$wordpairs=explode(";",$line);//split word pairs by each ;
foreach($wordpairs as $wordpair){
	$elements=explode(":",$wordpair);
	$word=$elements[0];
	$data[] = array(
		'label' => $searchTerm." ".$word,
		//'value' => $word
		'value' => $searchTerm." ".$word
    	);
}

// return JSON encoded array to the JQuery autocomplete function
echo json_encode($data);
flush();
?>

