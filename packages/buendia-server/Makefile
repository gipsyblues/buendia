include ../Makefile.inc

SOURCE_DIR=$(HOME)/openmrs/server/modules
TARGET_DIR=$(EXTRA_DATA)/usr/share/buendia/openmrs/modules
MAIN_MODULE=$(TARGET_DIR)/buendia-server.omod
OTHER_MODULES=$(TARGET_DIR)/xforms-4.3.5.omod $(TARGET_DIR)/webservices.rest-2.6.omod

$(EXTRA_DATA): $(MAIN_MODULE) $(OTHER_MODULES) $(EXTRA_DATA)/usr/bin/buendia-openmrs-account-setup $(EXTRA_DATA)/usr/bin/buendia-profile-apply $(EXTRA_DATA)/usr/bin/buendia-profile-validate

$(MAIN_MODULE):
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/projectbuendia.openmrs-1.0-SNAPSHOT.omod $@

$(OTHER_MODULES):
	mkdir -p $(TARGET_DIR)
	cp $(SOURCE_DIR)/$(notdir $@) $@

$(EXTRA_DATA)/usr/bin/buendia-openmrs-account-setup:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/openmrs_account_setup $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-profile-apply:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/profile_apply $@
	chmod 755 $@

$(EXTRA_DATA)/usr/bin/buendia-profile-validate:
	mkdir -p $$(dirname $@)
	cp -p $(TOOLS)/profile_validate $@
	chmod 755 $@
