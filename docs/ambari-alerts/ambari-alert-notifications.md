#Ambari Alert Notifications

For configuring alert notification during deployment, one should add the following lines to the blueprint:


    - type: io.brooklyn.ambari.AmbariCluster
      name: Ambari Cluster
      brooklyn.config:
        ambariAlertNotifications:
          name: <Default notification name> ### Notification Name
          description: <Default notification description - optional. If missing will use the default configured in brooklyn.properties>
          notification_type: EMAIL ### Notification type - OPTIONAL. If missing will use the default configured in brooklyn.properties
          alert_states: [OK, WARNING, CRITICAL, UNKNOWN] ### Alert states - OPTIONAL. If missing will use the default configured in brooklyn.properties
          properties:
            ambari.dispatch.recipients: [email.to@example.com, email2@example.com] ### Notification Recipients
            mail.smtp.from: email.from@example.com ### Mail from - OPTIONAL. If missing will use the default configured in brooklyn.properties
            mail.smtp.host: localhost ### Mail Host - OPTIONAL. If missing will use the default configured in brooklyn.properties
            mail.smtp.port: 587 ### Mail Port - OPTIONAL. If missing will use the default configured in brooklyn.properties


And put the following config in `brooklyn.properties` in order to set default values:

    ambari.alerts.notification.name=<Default notification name>
    ambari.alerts.notification.description=<Default notification description>
    ambari.alerts.notification.global=true
    ambari.alerts.notification.notification_type=EMAIL
    ambari.alerts.notification.alert_states=[OK, WARNING, CRITICAL, UNKNOWN]
    ambari.alerts.notification.default_properties={ ambari.dispatch.recipients: [email1@example.com, email2@example.com], mail.smtp.host: <Default smtp host>, mail.smtp.port: <Default smtp port>, mail.smtp.from: <Default smtp from>, mail.smtp.auth: false }
