package com.yubico.client.v2;

/**
 * Created by IntelliJ IDEA.
 * User: lwid
 * Date: 1/25/11
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public enum YubicoResponseStatus {
    OK,	/**The OTP is valid. */
    BAD_OTP,	/**The OTP is invalid format. */
    REPLAYED_OTP,	/**The OTP has already been seen by the service. */
    BAD_SIGNATURE,	/**The HMAC signature verification failed. */
    MISSING_PARAMETER,	/**The request lacks a parameter. */
    NO_SUCH_CLIENT,	/**The request id does not exist. */
    OPERATION_NOT_ALLOWED,	/**The request id is not allowed to verify OTPs. */
    BACKEND_ERROR,	/**Unexpected error in our server. Please contact us if you see this error. */
    NOT_ENOUGH_ANSWERS ,	/**Server could not get requested number of syncs during before timeout */
    REPLAYED_REQUEST	/**Server has seen the OTP/Nonce combination before */

}

