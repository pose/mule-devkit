/* file: mule-developer-core.css
   info: core developer styles
*/

/* RESET STYLES */

html, body, div, h1, h2, h3, h4, h5, h6, p, img,
dl, dt, dd, ol, ul, li, table, caption, tbody,
tfoot, thead, tr, th, td, form, fieldset,
embed, object, applet {
    margin: 0;
    padding: 0;
    border: 0;
}

/* BASICS */

html, body {
    overflow: hidden; /* keeps scrollbar off IE */
    background-color: #fff;
}

body {
    font-family: Tahoma, Arial, Verdana, sans-serif;
    color: #000;
    font-size: 13px;
    color: #333;
    background-image: url(assets/images/bg_fade.jpg);
    background-repeat: repeat-x;
}

a, a code {
    color: #006699;
}

a:active,
a:active code {
    color: #f00;
}

a:visited,
a:visited code {
    color: #006699;
}

input, select,
textarea, option, label {
    font-family: inherit;
    font-size: inherit;
    padding: 0;
    margin: 0;
    vertical-align: middle;
}

option {
    padding: 0 4px;
}

p {
    padding: 0;
    margin: 0 0 1em;
}

code, pre {
    color: #007000;
    font-family: Monaco, monospace;
    font-size: 12px;
    line-height: 1em;
}

var {
    color: #007000;
    font-style: italic;
}

pre {
    border: 1px solid #ccc;
    background-color: #fafafa;
    padding: 10px;
    margin: 0 0 1em 1em;
    overflow: auto;
    line-height: inherit; /* fixes vertical scrolling in webkit */
}

h1, h2, h3, h4, h5 {
    margin: 1em 0;
    padding: 0;
}

p, ul, ol, dl, dd, dt, li {
    line-height: 1.3em;
}

ul, ol {
    margin: 0 0 .8em;
    padding: 0 0 0 2em;
}

li {
    padding: 0 0 .5em;
}

dl {
    margin: 0 0 1em 0;
    padding: 0;
}

dt {
    margin: 0;
    padding: 0;
}

dd {
    margin: 0 0 1em;
    padding: 0 0 0 2em;
}

li p {
    margin: .5em 0 0;
}

dd p {
    margin: 1em 0 0;
}

li pre, li table, li img {
    margin: .5em 0 0 1em;
}

dd pre,
#jd-content dd table,
#jd-content dd img {
    margin: 1em 0 0 1em;
}

li ul,
li ol,
dd ul,
dd ol {
    margin: 0;
    padding: 0 0 0 2em;
}

li li,
dd li {
    margin: 0;
    padding: .5em 0 0;
}

dl dl,
ol dl,
ul dl {
    margin: 0 0 1em;
    padding: 0;
}

table {
    font-size: 1em;
    margin: 0 0 1em;
    padding: 0;
    border-collapse: collapse;
    border-width: 0;
    empty-cells: show;
}

td, th {
    border: 1px solid #ccc;
    padding: 6px 12px;
    text-align: left;
    vertical-align: top;
    background-color: inherit;
}

th {
    background-color: #ddd;
}

td > p:last-child {
    margin: 0;
}

hr.blue {
    background-color: #DDF0F2;
    border: none;
    height: 5px;
    margin: 20px 0 10px;
}

blockquote {
    margin: 0 0 1em 1em;
    padding: 0 4em 0 1em;
    border-left: 2px solid #eee;
}

nobr {
    font-family: Monaco, monospace;
}

/* LAYOUT */

#body-content {
    /* "Preliminary" watermark for draft documentation.
background:transparent url(images/preliminary.png) repeat scroll 0 0;  */
    margin: 0;
    position: relative;
    width: 100%;
}

#header {
    height: 88px;
    position: relative;
    z-index: 100;
    min-width: 675px; /* min width for the tabs, before they wrap */
    padding: 0 10px;
    border-bottom: 3px solid #3572A6;
}

#headerLeft {
    position: absolute;
    padding: 10px 0 0;
    left: 8px;
    bottom: 0px;
}

#headerRight {
    position: absolute;
    right: 0;
    bottom: 3px;
    padding: 0;
    text-align: right;
}

#masthead-title {
    font-family: Ubuntu, Arial, Helvetica;
    font-size: 28px;
    color: #2f74ae;
}

/* Tabs in the header */

#header ul {
    list-style: none;
    margin: 15px 0 0;
    padding: 0;
    height: 29px;
}

#header li {
    float: left;
    margin: 0px 2px 0px 0px;
    padding: 0;
}

#header li a {
    font-family: "Ubuntu", Arial, Helvetica;
    text-decoration: none;
    display: block;
    /* background-image: url(images/bg_images_sprite.png);
    background-position: 0 -58px;
    background-repeat: no-repeat; */
    color: #666;
    font-size: 13px;
    font-weight: normal;
	padding-left: 10px;
	padding-right: 10px;
    /* width: 94px; */
    height: 29px;
    text-align: center;
    margin: 0px;
	border-top: 1px solid #b5d0e8;
	border-left: 1px solid #b5d0e8;
	border-right: 1px solid #b5d0e8;
	border-top-right-radius: 7px;
	border-top-left-radius: 7px;
	
	/* Safari 4-5, Chrome 1-9 */
	background: -webkit-gradient(linear, 0% 0%, 0% 100%, from(#fdfeff), to(#ecf3f9));

	/* Safari 5.1, Chrome 10+ */
	background: -webkit-linear-gradient(top, #ecf3f9, #fdfeff);

	/* Firefox 3.6+ */
	background: -moz-linear-gradient(top, #ecf3f9, #fdfeff);

	/* IE 10 */
	background: -ms-linear-gradient(top, #ecf3f9, #fdfeff);

	/* Opera 11.10+ */
	background: -o-linear-gradient(top, #ecf3f9, #fdfeff);
}

#header li a:hover {
	/*
    background-image: url(images/bg_images_sprite.png);
    background-position: 0 -29px;
    background-repeat: no-repeat;
 	*/

	border-top: 1px solid #9dc1e0;
	border-left: 1px solid #9dc1e0;
	border-right: 1px solid #9dc1e0;
	border-top-right-radius: 7px;
	border-top-left-radius: 7px;
	
	/* Safari 4-5, Chrome 1-9 */cfe1f0
	background: -webkit-gradient(linear, 0% 0%, 0% 100%, from(#deeaf5), to(#cfe1f0));

	/* Safari 5.1, Chrome 10+ */
	background: -webkit-linear-gradient(top, #cfe1f0, #deeaf5);

	/* Firefox 3.6+ */
	background: -moz-linear-gradient(top, #cfe1f0, #deeaf5);

	/* IE 10 */
	background: -ms-linear-gradient(top, #cfe1f0, #deeaf5);

	/* Opera 11.10+ */
	background: -o-linear-gradient(top, #cfe1f0, #deeaf5);
}

#header li a:active {
	/*
    background-image: url(images/bg_images_sprite.png);
    background-position: 0 -29px;
    background-repeat: no-repeat;
 	*/
}

#header li a span {
    position: relative;
    top: 7px;
}

#header li a span+span {
    display: none;
}

/* tab highlighting */

.home #home-link a,
<?cs each:tab=tabs ?>.<?cs var:tab.id ?> #<?cs var:tab.id ?> a,<?cs /each ?>
.reference #reference-link a,
.sdk #sdk-link a,
.resources #resources-link a,
.videos #videos-link a {
    font-family: "Ubuntu", Arial, Helvetica;
	/*
    background-image: url(images/bg_images_sprite.png);
    background-position: 0 0;
    background-repeat: no-repeat;
	*/
    color: #fff;
    font-weight: normal;
    cursor: default;

	border-top: 1px solid #3777ae;
	border-left: 1px solid #3777ae;
	border-right: 1px solid #3777ae;
	border-top-right-radius: 7px;
	border-top-left-radius: 7px;
	
	/* Safari 4-5, Chrome 1-9 */
	background: -webkit-gradient(linear, 0% 0%, 0% 100%, from(#3572a6), to(#5191c8));

	/* Safari 5.1, Chrome 10+ */
	background: -webkit-linear-gradient(top, #5191c8, #3572a6);

	/* Firefox 3.6+ */
	background: -moz-linear-gradient(top, #5191c8, #3572a6);

	/* IE 10 */
	background: -ms-linear-gradient(top, #5191c8, #3572a6);

	/* Opera 11.10+ */
	background: -o-linear-gradient(top, #5191c8, #3572a6);
}

.home #home-link a:hover,
.reference #reference-link a:hover,
<?cs each:tab=tabs ?>.<?cs var:tab.id ?> #<?cs var:tab.id ?> a:hover,<?cs /each ?>
.sdk #sdk-link a:hover,
.resources #resources-link a:hover,
.videos #videos-link  a:hover {
	/*
    background-image: url(images/bg_images_sprite.png);
    background-position: 0 0;
	*/
	
	border-top: 1px solid #3777ae;
	border-left: 1px solid #3777ae;
	border-right: 1px solid #3777ae;
	border-top-right-radius: 7px;
	border-top-left-radius: 7px;
	
	/* Safari 4-5, Chrome 1-9 */
	background: -webkit-gradient(linear, 0% 0%, 0% 100%, from(#3572a6), to(#5191c8));

	/* Safari 5.1, Chrome 10+ */
	background: -webkit-linear-gradient(top, #5191c8, #3572a6);

	/* Firefox 3.6+ */
	background: -moz-linear-gradient(top, #5191c8, #3572a6);

	/* IE 10 */
	background: -ms-linear-gradient(top, #5191c8, #3572a6);

	/* Opera 11.10+ */
	background: -o-linear-gradient(top, #5191c8, #3572a6);	
}

#headerLinks {
    margin: 10px 10px 0 0;
    height: 13px;
    font-size: 11px;
    vertical-align: top;
}

#headerLinks a {
    color: #7FA9B5;
}

#headerLinks img {
    vertical-align: middle;
}

#language {
    margin: 0 10px 0 4px;
}

#search {
    margin: 8px 10px 0 0;
}

#mulesoftlogo {
    margin: 0px 10px 0 0;
}

/* MAIN BODY */

#mainBodyFluid {
    margin: 20px 10px;
    color: #333;
}

#mainBodyFixed {
    margin: 20px 10px;
    color: #333;
    width: 930px;
    position: relative;
}

#mainBodyFixed h3,
#mainBodyFluid h3 {
    color: #336666;
    font-size: 1.25em;
    margin: 0em 0em 0em 0em;
    padding-bottom: .5em;
}

#mainBodyFixed h2,
#mainBodyFluid h2 {
    color: #336666;
    font-size: 1.25em;
    margin: 0;
    padding-bottom: .5em;
}

#mainBodyFixed h1,
#mainBodyFluid h1 {
    color: #435A6E;
    font-size: 1.7em;
    margin: 1em 0;
}

#mainBodyFixed .green,
#mainBodyFluid .green,
#jd-content .green {
    color: #7BB026;
    background-color: none;
}

#mainBodyLeft {
    float: left;
    width: 600px;
    margin-right: 20px;
    color: #333;
    position: relative;
}

div.indent {
    margin-left: 40px;
    margin-right: 70px;
}

#mainBodyLeft p {
    color: #333;
    font-size: 13px;
}

#mainBodyLeft p.blue {
    color: #669999;
}

#mainBodyLeft #communityDiv {
    float: left;
    background-image: url(images/bg_community_leftDiv.jpg);
    background-repeat: no-repeat;
    width: 581px;
    height: 347px;
    padding: 20px 0px 0px 20px;
}

#mainBodyRight {
    float: left;
    width: 300px;
    color: #333;
}

#mainBodyRight p {
    padding-right: 50px;
    color: #333;
}

#mainBodyRight table {
    width: 100%;
}

#mainBodyRight td {
    border: 0px solid #666;
    padding: 0px 5px;
    text-align: left;
}

#mainBodyRight td p {
    margin: 0 0 1em 0;
}

#mainBodyRight .blueBorderBox {
    border: 5px solid #ddf0f2;
    padding: 18px 18px 18px 18px;
    text-align: left;
}

#mainBodyFixed .seperator {
    background-image: url(assets/images/hr_gray_side.jpg);
    background-repeat: no-repeat;
    width: 100%;
    float: left;
    clear: both;
}

#mainBodyBottom {
    float: left;
    width: 100%;
    clear: both;
    color: #333;
}

#mainBodyBottom .seperator {
    background-image: url(assets/images/hr_gray_main.jpg);
    background-repeat: no-repeat;
    width: 100%;
    float: left;
    clear: both;
}

/* FOOTER */

#footer {
    float: left;
    width: 90%;
    margin: 20px;
    color: #aaa;
    font-size: 11px;
}

#footer a {
    color: #aaa;
    font-size: 11px;
}

#footer a:hover {
    text-decoration: underline;
    color: #aaa;
}

#footerlinks {
    margin-top: 2px;
}

#footerlinks a,
#footerlinks a:visited {
    color: #006699;
}

/* SEARCH FILTER */

#search_autocomplete {
    color: #aaa;
}

#search-button {
    display: inline;
}

#search_filtered_div {
    position: absolute;
    margin-top: -1px;
    z-index: 101;
    border: 1px solid #BCCDF0;
    background-color: #fff;
}

#search_filtered {
    min-width: 100%;
}

#search_filtered td {
    background-color: #fff;
    border-bottom: 1px solid #669999;
    line-height: 1.5em;
}

#search_filtered .jd-selected {
    background-color: #94b922;
    cursor: pointer;
}

#search_filtered .jd-selected,
#search_filtered .jd-selected a {
    color: #fff;
}

.no-display {
    display: none;
}

.jd-autocomplete {
    font-family: Arial, sans-serif;
    padding-left: 6px;
    padding-right: 6px;
    padding-top: 1px;
    padding-bottom: 1px;
    font-size: 0.81em;
    border: none;
    margin: 0;
    line-height: 1.05em;
}

.show-row {
    display: table-row;
}

.hide-row {
    display: hidden;
}

/* SEARCH */

/* restrict global search form width */
#searchForm {
    width: 350px;
}

#searchTxt {
    width: 200px;
}

/* disable twiddle and size selectors for left column */
#leftSearchControl div {
    width: 100%;
}

#leftSearchControl .gsc-twiddle {
    background-image: none;
}

#leftSearchControl td, #searchForm td {
    border: 0px solid #000;
}

#leftSearchControl .gsc-resultsHeader .gsc-title {
    padding-left: 0px;
    font-weight: bold;
    font-size: 13px;
    color: #006699;
    display: none;
}

#leftSearchControl .gsc-resultsHeader div.gsc-results-selector {
    display: none;
}

#leftSearchControl .gsc-resultsRoot {
    padding-top: 6px;
}

#leftSearchControl div.gs-visibleUrl-long {
    display: block;
    color: #006699;
}

.gsc-webResult div.gs-visibleUrl-short,
table.gsc-branding,
.gsc-clear-button {
    display: none;
}

.gsc-cursor-box .gsc-cursor div.gsc-cursor-page,
.gsc-cursor-box .gsc-trailing-more-results a.gsc-trailing-more-results,
#leftSearchControl a,
#leftSearchControl a b {
    color: #006699;
}

.gsc-resultsHeader {
    display: none;
}

/* Disable built in search forms */
.gsc-control form.gsc-search-box {
    display: none;
}

table.gsc-search-box {
    margin: 6px 0 0 0;
    border-collapse: collapse;
}

td.gsc-input {
    padding: 0 2px;
    width: 100%;
    vertical-align: middle;
}

input.gsc-input {
    border: 1px solid #BCCDF0;
    width: 99%;
    padding-left: 2px;
    font-size: .95em;
}

td.gsc-search-button {
    text-align: right;
    padding: 0;
    vertical-align: top;
}

#search-button {
    margin: 0 0 0 2px;
    font-size: 11px;
}

/* search result tabs */

#doc-content .gsc-control {
    position: relative;
}

#doc-content .gsc-tabsArea {
    position: relative;
    white-space: nowrap;
}

#doc-content .gsc-tabHeader {
    padding: 3px 6px;
    position: relative;
}

#doc-content .gsc-tabHeader.gsc-tabhActive {
    border-top: 2px solid #94B922;
}

#doc-content h2#searchTitle {
    padding: 0;
}

#doc-content .gsc-resultsbox-visible {
    padding: 1em 0 0 6px;
}

/* Pretty printing styles. Used with prettify.js. */

.str {
    color: #080;
}

.kwd {
    color: #008;
}

.com {
    color: #800;
}

.typ {
    color: #606;
}

.lit {
    color: #066;
}

.pun {
    color: #660;
}

.pln {
    color: #000;
}

dl.tag-list dt code,
.tag {
    color: #008;
}

dl.atn-list dt code,
.atn {
    color: #828;
}

.atv {
    color: #080;
}

.dec {
    color: #606;
}

@media print {
    .str {
        color: #060;
    }

    .kwd {
        color: #006;
        font-weight: bold;
    }

    .com {
        color: #600;
        font-style: italic;
    }

    .typ {
        color: #404;
        font-weight: bold;
    }

    .lit {
        color: #044;
    }

    .pun {
        color: #440;
    }

    .pln {
        color: #000;
    }

    .tag {
        color: #006;
        font-weight: bold;
    }

    .atn {
        color: #404;
    }

    .atv {
        color: #060;
    }
}