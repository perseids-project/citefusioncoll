function loadXMLDoc(url, xsl, elemId){
	var ctsResponse;
	var xmlhttp = new XMLHttpRequest();  
	xmlhttp.open("GET", url, true);
	xmlhttp.send('');  
	
	xmlhttp.onreadystatechange = function() {  
		if(xmlhttp.readyState == 4) {
  		  putTextOntoPage("Finished loading text!", elemId);
		  ctsResponse = xmlhttp.responseXML; 
		  nowGetXSLT(ctsResponse, xsl, elemId);
  		}
	}; 	
}


function nowGetXSLT(ctsResponse, xsl, elemId){
	var myURL = xsl;
	
	var xslhttp = new XMLHttpRequest();  
	xslhttp.open("GET", xsl, true);
	xslhttp.send('');  
	
	xslhttp.onreadystatechange = function() {  
		if(xslhttp.readyState == 4) {
		  putTextOntoPage("<p>Finished loading xslt!</p>", elemId);
		  xsltData = xslhttp.responseXML;   		
		  processXML(ctsResponse, xsltData, elemId);
  		}	
	}; 
}

function processXML(ctsResponse, xsltData, elemId){
		var processor = null;
		var tempData = null;
		var tempHTML = "";
		processor = new XSLTProcessor();
		processor.importStylesheet(xsltData);
		tempData = processor.transformToDocument(ctsResponse);
		tempHTML = new XMLSerializer().serializeToString(tempData);	
		putTextOntoPage(tempHTML, elemId);
}



function putTextOntoPage(htmlText, elemId){
	document.getElementById(elemId).innerHTML = htmlText;
	$("#" + elemId).removeClass("waiting");
	// Catch any Markdown fields
	processMarkdown(elemId);
}

function processMarkdown(elemId){
    $(".md").each( function(i) {
		var mdText = $(this).html().trim();
		$(this).html(markdown.toHTML(mdText));
		//Remove class, so this doesn't get double-processed
		$(this).removeClass("md");

	});
	
	
	
// 	
// 	if ( $("#" + elemId).find(".md").html() != null ) {
// 		console.log("not null");
// 		var mdText = $("#" + elemId).find(".md").html().trim();
// 		console.log(mdText);
// 		$("#" + elemId).find(".md").html(markdown.toHTML(mdText));
// 	
// 	}
}

function assignIds(whichClass){
	$('.' + whichClass).each(function(index){
		$(this).addClass("waiting");
		$(this).html(" ");
		
		$(this).attr("id","cts_text_" + index);
		$(this).wrapInner("<a/>");
		if ( !($(this).hasClass("cts-clicked")) ){
				$(this).addClass("cts-clicked"); // prevent it from re-loading on every subsequent click
				var thisLink;
				if (  $(this).attr("cite").substring(0,7) == "http://"  ) {
					var tempPart = ( $(this).attr("cite").substring(7,$(this).attr("cite").length) );
					thisLink = "http://" + encodeURIComponent(tempPart);
					thisLink = $(this).attr("cite");
					
				} else {
					thisLink = urlOfCTS + $(this).attr("cite");
				}
				loadXMLDoc( thisLink, pathToXSLT, $(this).attr("id"));
			}
		
	});
}

function assignCiteIds(whichClass){
	$('.' + whichClass).each(function(index){
		$(this).addClass("waiting");
		$(this).html(" ");
		$(this).attr("id","cite_coll_" + index);
		$(this).wrapInner("<a/>");
		if ( !($(this).hasClass("cts-clicked")) ){
				$(this).addClass("cts-clicked"); // prevent it from re-loading on every subsequent click
				var thisLink;
				if (  $(this).attr("cite").substring(0,7) == "http://"  ) {
					thisLink = $(this).attr("cite");
					
				} else {
					thisLink = urlOfCite + $(this).attr("cite");
				}
				loadXMLDoc( thisLink, pathToCiteXSLT, $(this).attr("id"));
			}
		
	});
}

function fixImages(whichClass){
	$('.' + whichClass).each(function(index){
		tempAttr = $(this).attr("src");
		
		if ($(this).attr("src").substring(0,7) != "http://" ) {
			$(this).attr("src",urlOfImgService + tempAttr + "&w=" + imgSize + "&request=GetBinaryImage");
			tempZoomUrl = urlOfImgService + tempAttr + "&request=GetIIPMooViewer";
			$(this).wrap("<a href='" + tempZoomUrl + "'/>");
		} else {
			var tempUrl = $(this).attr("src");
			console.log(tempUrl);
			$(this).replaceWith("<blockquote class='cite-img' id='citeimg_" + index + "'>" + tempUrl +  "</blockquote>");
			loadXMLDoc( tempUrl, pathToImgXSLT, "citeimg_" + index);
		}
	});
}

$(document).ready(function(){
   processMarkdown("article");
   assignIds(textElementClass);	
   assignCiteIds(collectionElementClass);
   fixImages(imgElementClass);	   
});
