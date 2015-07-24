package com.yubico.client.v2;

import static com.google.common.base.Preconditions.checkArgument;

public class OtpUtils {

	/**
	 * Extract the public ID of a YubiKey from an OTP it generated.
	 *
	 * @param otp  the OTP to extract ID from, in modhex format.
	 * @return string	the public ID of YubiKey that generated the given otp. Between 0 and 12 lower-case characters.
	 * @throws IllegalArgumentException for arguments that are null or too short to be valid OTP strings.
	 */
	public static String getPublicId(String otp) {
		checkArgument(otp.length() >= OTP_MIN_LEN, "The OTP is too short to be valid");

		/* The OTP part is always the last 32 bytes of otp. Whatever is before that
		 * (if anything) is the public ID of the YubiKey. The ID can be set to ''
		 * through personalization.
		 */
		return otp.substring(0, otp.length() - 32).toLowerCase();
	}

	private static final Integer OTP_MIN_LEN = 32;
	private static final Integer OTP_MAX_LEN = 48;
	/**
	 * Determines whether a given OTP is of the correct length
	 * and only contains printable characters, as per the recommendation.
	 *
	 * @param otp The OTP to validate
	 * @return boolean Returns true if it's valid; false otherwise
	 *
	 */
	public static boolean isValidOTPFormat(String otp) {
		return otp != null
				&& OTP_MIN_LEN <= otp.length()
				&& otp.length() <= OTP_MAX_LEN
				&& otp.chars().allMatch(c -> c >= 0x20 && c <= 0x7E);
	}
}
