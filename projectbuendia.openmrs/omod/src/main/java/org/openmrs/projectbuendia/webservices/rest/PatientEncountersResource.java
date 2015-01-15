package org.openmrs.projectbuendia.webservices.rest;

import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.projectbuendia.DateTimeUtils;
import org.openmrs.projectbuendia.VisitObsValue;
import org.projectbuendia.openmrs.webservices.rest.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only resource representing multiple observations for a single patient.
 */
// TODO(jonskeet): Ideally, this would be under patient/{uuid}/encounters; it's unclear whether
// that can be supported here...
@Resource(name = RestController.REST_VERSION_1_AND_NAMESPACE + "/patientencounters", supportedClass = Patient.class, supportedOpenmrsVersions = "1.10.*,1.11.*")
public class PatientEncountersResource extends AbstractReadOnlyResource<Patient> {

    // JSON property names
    private static final String UUID = "uuid";
    private static final String ENCOUNTERS = "encounters";
    private static final String OBSERVATIONS = "observations";
    private static final String TIMESTAMP = "timestamp";
    public static final String START_MILLISECONDS_INCLUSIVE = "sm";

    private final PatientService patientService;
    private final EncounterService encounterService;

    public PatientEncountersResource() {
        super("patient", Representation.DEFAULT);
        patientService = Context.getPatientService();
        encounterService = Context.getEncounterService();
    }
    
    @Override
    protected Patient retrieveImpl(String uuid, RequestContext context, long snapshotTime) {
        return patientService.getPatientByUuid(uuid);
    }
    
    @Override
    public List<Patient> searchImpl(RequestContext context, long snapshotTime) {
        return patientService.getAllPatients();
    }

    @Override
    protected void populateJsonProperties(Patient patient, RequestContext context, SimpleObject json,
                                          long snapshotTime) {
        String parameter = context.getParameter(START_MILLISECONDS_INCLUSIVE);
        Long startMillisecondsInclusive = null;
        if (parameter != null) {
            // Fail fast throwing number format exception to aid debugging.
            startMillisecondsInclusive = Long.parseLong(parameter);
        }
        List<Encounter> encountersByPatient;
        if (startMillisecondsInclusive == null) {
            encountersByPatient = encounterService.getEncountersByPatient(patient);
        } else {
            /* It would be nice to be able to use the getEncounters() method here, which has the following parameters.
             * Unfortunately the date restrictions are on the encounter date, not on the creation/modification date.
             * This means we would not get encounters added in the past by a later sync. Using creation/modification
             * date as a feature has been added as a feature request to
             * OpenMRS at https://issues.openmrs.org/browse/TRUNK-4571
             *
             * Until this is done, we have two options. 1 Use the DAO directly, hooking in to the spring injection
             * code to get it. 2 load the encounters, and then filter before getting the observations, hoping this is
             * efficient enough. For now we are going with 2.
             *
             * Nullable parameters for getEncounters(), put here for easier readability:
             * who - the patient the encounter is for
             * loc - the location this encounter took place
             * fromDate - the minimum date (inclusive) this encounter took place
             * toDate - the maximum date (exclusive) this encounter took place
             * enteredViaForms - the form that entered this encounter must be in this list
             * encounterTypes - the type of encounter must be in this list
             * providers - the provider of this encounter must be in this list
             * visitTypes - the visit types of this encounter must be in this list
             * visits - the visits of this encounter must be in this list
             * includeVoided - true/false to include the voided encounters or not
             */
            encountersByPatient = filterEncountersByModificationTime(startMillisecondsInclusive,
                    encounterService.getEncountersByPatient(patient));
        }
        List<SimpleObject> encounters = new ArrayList<>();
        for (Encounter encounter : filterBeforeSnapshotTime(snapshotTime, encountersByPatient)) {
            encounters.add(encounterToJson(encounter));
        }
        json.put(ENCOUNTERS, encounters);
    }

    private List<Encounter> filterEncountersByModificationTime(Long startMillisecondsInclusive,
                                                               List<Encounter> encountersByPatient) {
        ArrayList<Encounter> filtered = new ArrayList<>();
        for (Encounter encounter : encountersByPatient) {
            if (encounter.getDateCreated().getTime() >= startMillisecondsInclusive ||
                    (encounter.getDateChanged() != null &&
                            encounter.getDateChanged().getTime() >= startMillisecondsInclusive)) {
                filtered.add(encounter);
            }
        }
        return filtered;
    }

    private List<Encounter> filterBeforeSnapshotTime(long snapshotTime,
                                                     List<Encounter> encountersByPatient) {
        ArrayList<Encounter> filtered = new ArrayList<>();
        for (Encounter encounter : encountersByPatient) {
            if (encounter.getDateCreated().getTime() < snapshotTime) {
                filtered.add(encounter);
            }
        }
        return filtered;
    }

    private SimpleObject encounterToJson(Encounter encounter) {
        SimpleObject encounterJson = new SimpleObject();
        // TODO: Check what format this ends up in.
        encounterJson.put(TIMESTAMP, DateTimeUtils.toIso8601(encounter.getEncounterDatetime()));
        SimpleObject observations = new SimpleObject();
        for (Obs obs : encounter.getObs()) {
            encounterJson.put(UUID, encounter.getUuid());
            Concept concept = obs.getConcept();
            String value = VisitObsValue.visit(obs, new VisitObsValue.ObsValueVisitor<String>() {
                @Override
                public String visitCoded(Concept value) {
                    return value.getUuid();
                }

                @Override
                public String visitNumeric(Double value) {
                    return String.valueOf(value);
                }

                @Override
                public String visitBoolean(Boolean value) {
                    return String.valueOf(value);
                }

                @Override
                public String visitText(String value) {
                    return value;
                }
            });
            observations.put(concept.getUuid(), value);
        }
        encounterJson.put(OBSERVATIONS, observations);        
        return encounterJson;
    }
}
