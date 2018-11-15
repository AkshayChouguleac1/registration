package io.mosip.registration.service.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.constants.RegistrationConstants.MACHINE_MAPPING_ENTITY_ERROR_NO_RECORDS;
import static io.mosip.registration.constants.RegistrationConstants.MACHINE_MAPPING_ENTITY_SUCCESS_MESSAGE;
import static io.mosip.registration.constants.RegistrationConstants.MACHINE_MAPPING_LOGGER_TITLE;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.spi.logger.MosipLogger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UserMachineMappingDTO;
import io.mosip.registration.entity.RegistrationUserDetail;
import io.mosip.registration.entity.UserMachineMapping;
import io.mosip.registration.entity.UserMachineMappingID;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.MapMachineService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

/**
 * User Client Machine Mapping Service
 * 
 * @author Yaswanth S
 * @since 1.0.0
 *
 */
@Service
public class MapMachineServiceImpl implements MapMachineService {

	/**
	 * LOGGER for logging
	 */
	private static final MosipLogger LOGGER = AppConfig.getLogger(MapMachineServiceImpl.class);

	@Autowired
	private MachineMappingDAO machineMappingDAO;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.MapMachineService#saveOrUpdate(io.mosip.
	 * registration.dto.UserMachineMappingDTO)
	 */
	@Override
	public ResponseDTO saveOrUpdate(UserMachineMappingDTO userMachineMappingDTO) {
		LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
				"Service saveOrUpdate method called");

		// find user
		UserMachineMappingID userID = new UserMachineMappingID();
		userID.setUserID(userMachineMappingDTO.getUserID());
		userID.setCentreID(userMachineMappingDTO.getCentreID());
		userID.setMachineID(userMachineMappingDTO.getStationID());

		boolean isActive = userMachineMappingDTO.getStatus()
				.equalsIgnoreCase(RegistrationConstants.MACHINE_MAPPING_ACTIVE);

		/* create response */
		ResponseDTO responseDTO = new ResponseDTO();

		/* Interacting with DAO layer */

		try {
			/* find user */
			UserMachineMapping user = machineMappingDAO.findByID(userID);

			if (user != null) {
				/* if user already exists */
				user.setUpdBy(SessionContext.getInstance().getUserContext().getUserId());
				user.setUpdDtimes(new Timestamp(System.currentTimeMillis()));
				user.setIsActive(isActive);

				user.setIsActive(isActive);
				machineMappingDAO.update(user);
			} else {
				/* if user didn't exists */
				user = new UserMachineMapping();
				user.setUserMachineMappingId(userID);
				user.setIsActive(isActive);
				user.setCrBy(SessionContext.getInstance().getUserContext().getUserId());
				user.setCrDtime(new Timestamp(System.currentTimeMillis()));
				user.setUpdBy(SessionContext.getInstance().getUserContext().getUserId());
				user.setUpdDtimes(new Timestamp(System.currentTimeMillis()));

				machineMappingDAO.save(user);
			}
			/* create success response */
			SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
			successResponseDTO.setCode(RegistrationConstants.MACHINE_MAPPING_CODE);
			successResponseDTO.setInfoType(RegistrationConstants.ALERT_INFORMATION);
			successResponseDTO.setMessage(RegistrationConstants.MACHINE_MAPPING_SUCCESS_MESSAGE);
			responseDTO.setSuccessResponseDTO(successResponseDTO);
			LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID, "Success Response created");
		} catch (RegBaseUncheckedException exception) {
			responseDTO = getErrorResponse(responseDTO, RegistrationConstants.MACHINE_MAPPING_ERROR_MESSAGE);
			LOGGER.error(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID, "Error Response created");

		}
		LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
				"Service saveOrUpdate method ended");

		return responseDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.MapMachineService#view()
	 */
	@Override
	public ResponseDTO view() {
		LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID, "View Method called");

		ResponseDTO responseDTO = new ResponseDTO();

		try {
			/* get mac address */
			String machineID = RegistrationSystemPropertiesChecker.getMachineId();
			/* get station ID */
			String stationID = machineMappingDAO.getStationID(machineID);
			/* get center id */
			String centerID = machineMappingDAO.getCenterID(stationID);
			/* get user list */
			List<RegistrationUserDetail> registrationUserDetails = machineMappingDAO.getUsers(centerID);

			if (registrationUserDetails != null && !registrationUserDetails.isEmpty()) {
				/* create success response */
				SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
				successResponseDTO.setCode(RegistrationConstants.MACHINE_MAPPING_CODE);
				successResponseDTO.setMessage(MACHINE_MAPPING_ENTITY_SUCCESS_MESSAGE);
				successResponseDTO
						.setOtherAttributes(constructDTOs(machineID, stationID, centerID, registrationUserDetails));

				responseDTO.setSuccessResponseDTO(successResponseDTO);
				LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
						"View Method Success Response created");
			} else {
				getErrorResponse(responseDTO, MACHINE_MAPPING_ENTITY_ERROR_NO_RECORDS);
			}
		} catch (RegBaseUncheckedException regBaseUncheckedException) {
			responseDTO = getErrorResponse(responseDTO, regBaseUncheckedException.getMessage());
			LOGGER.error(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
					"View() Method Error " + regBaseUncheckedException.getMessage());
		} catch (RegBaseCheckedException regBaseCheckedException) {
			responseDTO = getErrorResponse(responseDTO, regBaseCheckedException.getMessage());
			LOGGER.error(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
					"Exception Method Response created" + regBaseCheckedException.getMessage());
		}

		return responseDTO;
	}

	/**
	 * To prepare List of {@link UserMachineMappingDTO}
	 * 
	 * @param machineID
	 * @param stationID
	 * @param centreID
	 * @param registrationUserDetails
	 * @return
	 */
	private Map<String, Object> constructDTOs(String machineID, String stationID, String centreID,
			List<RegistrationUserDetail> registrationUserDetails) {
		LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID, "constructDTOs() method called");
		Map<String, Object> userDetailMap = new HashMap<>();
		try {
			List<UserMachineMappingDTO> userMachineMappingDTOs = registrationUserDetails.stream()
					.map(registrationUserDetail -> {
						UserMachineMappingDTO userMachineMappingDTO = null;
						if (registrationUserDetail != null) {
							userMachineMappingDTO = constructDTO(registrationUserDetail, machineID, stationID,
									centreID);
						}
						return userMachineMappingDTO;
					}).collect(Collectors.toList());
			userDetailMap.put(RegistrationConstants.USER_MACHINE_MAPID, userMachineMappingDTOs);
		} catch (RegBaseUncheckedException regBaseUncheckedException) {
			LOGGER.error(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID,
					"Exception in preparing DTO " + regBaseUncheckedException.getMessage());
		}
		return userDetailMap;
	}

	/**
	 * To prepare the {@link UserMachineMappingDTO}
	 * 
	 * @param machineID
	 * @param stationID
	 * @param centreID
	 * @param registrationUserDetails
	 * @return
	 */
	private UserMachineMappingDTO constructDTO(RegistrationUserDetail registrationUserDetail, String machineID,
			String stationID, String centreID) {
		LOGGER.debug(MACHINE_MAPPING_LOGGER_TITLE, APPLICATION_NAME, APPLICATION_ID, "constructDTO() method called");
		String userID = registrationUserDetail.getId();
		String userName = registrationUserDetail.getName();
		StringBuilder role = new StringBuilder();
		String roleCode = "";
		String status = RegistrationConstants.USER_IN_ACTIVE;
		
		if (!registrationUserDetail.getUserRole().isEmpty()) {
			/* List of roles with comma separated */
			registrationUserDetail.getUserRole().forEach(registrationUserRole -> role
					.append(registrationUserRole.getRegistrationUserRoleID().getRoleCode() + ","));
			if (role.length() > 0) {
				roleCode = role.substring(0, role.lastIndexOf(","));
			}
		}
		if (!registrationUserDetail.getUserMachineMapping().isEmpty()) {
			for (UserMachineMapping userMachineMapping : registrationUserDetail.getUserMachineMapping()) {
				if (userMachineMapping.getUserMachineMappingId().getMachineID().equals(stationID)) {
					status = userMachineMapping.getIsActive() ? RegistrationConstants.USER_ACTIVE
							: RegistrationConstants.USER_IN_ACTIVE;
				}
			}
		}

		return new UserMachineMappingDTO(userID, userName, roleCode, status, centreID, stationID, machineID);
	}

	/**
	 * Common method to prepare error response
	 * 
	 * @param response
	 * @param message
	 * @return
	 */
	private ResponseDTO getErrorResponse(ResponseDTO response, final String message) {
		/* Create list of Error Response */
		LinkedList<ErrorResponseDTO> errorResponses = new LinkedList<>();

		/* Error response */
		ErrorResponseDTO errorResponse = new ErrorResponseDTO();
		errorResponse.setCode(RegistrationConstants.MACHINE_MAPPING_CODE);
		errorResponse.setInfoType(RegistrationConstants.ALERT_ERROR);
		errorResponse.setMessage(message);

		errorResponses.add(errorResponse);

		/* Assing list of error responses to response */
		response.setErrorResponseDTOs(errorResponses);
		return response;

	}
}
