@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "common",
                "vacancy",
                "recruitment",
                "recruitment::event",
                "recruitment::model"
        }
)
package ua.edu.ukma.candidai.assessment;
