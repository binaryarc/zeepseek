import "./AlertModal.css";
import zeeplogo from "../../assets/images/mypage.png"

const AlertModal = ({ message = "완료되었습니다!", buttonText = "확인", onClose }) => {
  return (
    <div className="alert-overlay">
      <div className="alert-box">
        <img src={zeeplogo} alt="icon" className="alert-icon" />
        <p>{message}</p>
        <button onClick={onClose}>{buttonText}</button>
      </div>
    </div>
  );
};

export default AlertModal;
