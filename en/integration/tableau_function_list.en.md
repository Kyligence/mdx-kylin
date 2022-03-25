## Supported Tableau Functionality

### Prerequisite

Below functionality requires Windows Tableau 10.x and above



### Data Connection

| **Function**                         | **Support Status** | **Operation Path**                                           |
| :----------------------------------- | :----------------- | :----------------------------------------------------------- |
| Use Windows Authentication           | Not Support        | Connect - To a Server - More - Microsoft Analysis Services - Use Windows Authentication |
| Use a specific username and password | Support            | Connect - To a Server - More - Microsoft Analysis Services - Use a specific username and password |

 

### Sync Semantic Model

| **Function**       | **Support Status** | **Comment**                                                  |
| :----------------- | :----------------- | :----------------------------------------------------------- |
| Dimension Table    | Support            |                                                              |
| Hierarchy          | Support            |                                                              |
| Dimension          | Support            |                                                              |
| Measure            | Support            |                                                              |
| Calculated Measure | Support            | Measures and calculated meausres are shown in the mesure panel |
| Named Set          | Support            |                                                              |
| Folder             | Not Support        | Folders of dimensions and measures are not synchronized      |

 

### Field Select & Search

| **Function** | **Support Status** | **Comment**                                           |
| :----------- | :----------------- | :---------------------------------------------------- |
| Filters      | Support            | Support filtering on dimension, hierarchy and measure |
| Rows         | Support            | Support choosing dimension, hierarchy and measure     |
| Columns      | Support            | Support choosing dimension, hierarchy and measure     |
| Sheet area   | Support            |                                                       |
| Field Search | Support            | Support searching dimension, hierarchy and measure    |

 

### Filtering

| **Function**                        | **Support Status** | **Comment**                                                  |
| :---------------------------------- | :----------------- | :----------------------------------------------------------- |
| Filter on dimension                 | Support            | Support General, Condition, Top filter. Support single/multiple values filtering. Support searching value on filters |
| Filter on hierarchy                 | Support            | Support General, Condition, Top filter. Support single/multiple values filtering. Support searching value on filters |
| Filter on measure/calculetd measure | Support            | Sopport Range of laues, At least, At most, Special filtering |

 

### Sorting

| **Function**                      | **Support Status** | **Comment**                                                  |
| :-------------------------------- | :----------------- | :----------------------------------------------------------- |
| Sorting dimension/hierarchy       | Support            | Support sorting from an axis, header, field label, or toolbar, or manual sorting. Support sorting by Data source order, Alphabetic, Field, Manual, or Nested |
| Sorting measure/calculetd measure | Support            | Support sorting from an axis, header, field label, or toolbar, or manual sorting |

 

### Layout & Display

| **Function**   | **Support Status** | **Operation Path**                    | **Comment**                                                  |
| :------------- | :----------------- | :------------------------------------ | :----------------------------------------------------------- |
| Format setting | Support            | Right-click menu - Format             | Formating such as currency sign $ or￥can be set by editing the dataset, or it can be set in Tableau |
| Show Totals    | Support            | Analysis - Totals                     | Columns with small cardinality placed on the left has better performance |
| Show Subtotals | Support            | Analysis - Totals - Add All Subtotals | Columns with small cardinality placed on the left has better performance |

 

### Data Refresh

| **Function**          | **Support Status** | **Operation Path**                         | **Comment**                                           |
| :-------------------- | :----------------- | :----------------------------------------- | :---------------------------------------------------- |
| Auto Update Worksheet | Support            | Toolbar - Auto Update Worksheet            |                                                       |
| Data Source - Refresh | Support            | Data - the specified data source - Refresh | Will update all worksheets using the same data source |



### Dashboard

| **Function**        | **Support Status** | **Comment**                                    |
| :------------------ | :----------------- | :--------------------------------------------- |
| Filter Actions      | Support            | Dashboard - Actions - Add Action - Filter      |
| Highlight Actions   | Support            | Dashboard - Actions - Add Action - Highlight   |
| Go to URL Actions   | Support            | Dashboard - Actions - Add Action - Go to URL   |
| Go to Sheet Actions | Support            | Dashboard - Actions - Add Action - Go to Sheet |