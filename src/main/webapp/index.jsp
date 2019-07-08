<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<jsp:useBean id="myoauth" class="com.yanmouxie.oauth2.Test"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Yanmouxie.com</title>
</head>

<body>
	<H1>Hello world!</H1> 
	<a href="http://Yanmouxie.com">Yanmouxie.com</a><br/>
	<br/>
	Access Token:<br/>
	<form action=""><input type="text" name="token" value="<%=myoauth.requestAccessToken("myclient","mysecret")%>" /> <input type="submit" name="submit" value="Check Token"></form><br/>
	Result:
	<%
	if(request.getParameter("token")!=null && !"".equals(request.getParameter("token")))
	{
		out.println(myoauth.check(request.getParameter("token")) );
	}
	%>
</body>
</html>