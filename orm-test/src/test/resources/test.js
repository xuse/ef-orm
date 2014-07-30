function main() {
	var xml = <GetQuoteResponse xmlns="http://ws.invesbot.com/"
	  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
	  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	  <GetQuoteResult>
	    <StockQuote xmlns="">
	      <Symbol>GOOG</Symbol>
	      <Company>GOOGLE</Company>
	      <Price>&lt;big&gt;&lt;b&gt;417.94&lt;/b&gt;&lt;/big&gt;</Price>
	      <Time>Apr 28</Time>
	      <Change>
	        &lt;img width="10" height="14" border="0"
	        src="http://us.i1.yimg.com/us.yimg.com/i/us/fi/03rd/down_r.gif"
	        alt="Down"&gt;&amp;nbsp;&lt;b style="color:#cc0000;"&gt;2.09
	        (0.50%)&lt;/b&gt;
	      </Change>
	      <PrevClose>420.03</PrevClose>
	      <Open>417.73</Open>
	      <Bid>417.55&lt;small&gt; x 100&lt;/small&gt;</Bid>
	      <Ask>417.99&lt;small&gt; x 100&lt;/small&gt;</Ask>
	      <YearTarget>495.38</YearTarget>
	      <DayRange>416.30 - 425.73</DayRange>
	      <YearRange>217.82 - 475.11</YearRange>
	      <Volume>7,425,666</Volume>
	      <AvgVol>13,020,000</AvgVol>
	      <MarketCap>124.23B</MarketCap>
	      <PE>73.32</PE>
	      <EPS>5.70</EPS>
	      <DivYield>N/A (N/A)</DivYield>
	      <WebSite>
	        &lt;a
	        href="http://www.google.com"&gt;http:// www.google.com&lt;/a&gt;
	      </WebSite>
	      <Business>
	        Google, Inc. offers advertising and Internet search solutions,
	        as well as intranet solutions through an enterprise search
	        appliance. The company, through Google.com, provides Google
	        WebSearch that offers access to Web pages; Google Image Search,
	        a searchable index of images found across the Web; Google Groups
	        that enable participation in Internet discussion groups; Google
	        News that gathers information from news sources and presents
	        news in a searchable format; Froogle, a shopping search engine;
	        Google Local that allows users to find driving directions and
	        local businesses; and Google Desktop that enables users to
	        perform a text search on the contents of their own computer. Its
	        Web and content search products include Google Scholar to search
	        for scholarly literature; Google Book Search to bring print
	        information online; Google Base to upload, store, and describe
	        online or offline content; and Google Video to exchange video
	        content between consumers and producers. These products also
	        comprise personalized search and homepage, alerts, Web
	        directory, and music search. The company?s communication and
	        collaboration products and services comprise Gmail, an email
	        service; orkut that enables users to search and connect to other
	        users; and Blogger, a Web-based publishing tool. Google also
	        provides downloadable applications for computers; ability to
	        search and view mobile Web, and a downloadable Java client
	        application for mobiles; and Google Labs that operate as test
	        beds for its engineers and users. Further, the company offers
	        Google AdWords, an advertising program that presents ads to
	        people; Google AdSense program that allows Web sites in the
	        Google Network to serve targeted ads from AdWords advertisers;
	        and search technology for enterprises through the Google Search
	        Appliance and Google Mini. Google was founded by Larry Page and
	        Sergey Brin in 1998. The company is headquartered in Mountain
	        View, California. Google acquired dMarc Broadcasting, Inc. in
	        February 2006.
	      </Business>
	      <sic />
	      <sicname />
	    </StockQuote>
	  </GetQuoteResult>
	</GetQuoteResponse>

	out.println(xml..Open.text());
	return "";
}