WMS Premium UI — Java / Vaadin only

Included: WmsDesign.java, MainView.java, DashboardView.java, InventoryView.java,
OrderView.java, ProductView.java, CustomerView.java.

BEFORE replacing: back up your current project and database.
Copy all 7 Java files into:
C:\Users\admin\skeleton-starter-flow-spring-24\src\main\java\org\vaadin\example\

This is a code update, not a compiled/tested production release.
No service/repository files or MySQL schema are changed. Existing Inventory,
Order, Product and Customer management methods were retained from your upload;
only the WmsDesign.decorate(...) invocation was added to their constructors.

Security: Products/Customers retain @RolesAllowed("ADMIN"); Inventory/Orders
retain their existing @PermitAll. Sidebar hides admin pages for STAFF.

In PowerShell from the Spring Boot project folder:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean compile

After successful compilation, close any running WMS backend/desktop server,
then .\mvnw.cmd package -Pproduction and launch the resulting JAR.
Update the desktop WMS.jar copy only after testing localhost.

Login was NOT changed because LoginView/SecurityConfig was not supplied.
Spring Security's login screen remains as it is; request separate login redesign
only after its source and security config are inspected.
