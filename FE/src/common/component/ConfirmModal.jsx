import "./ConfirmModal.css";
import zeeplogo from "../../assets/images/mypage.png"

const ConfirmModal = ({ message, onConfirm, onCancel }) => {
  return (
    <div className="confirm-alert-overlay">
      <div className="confirm-alert-box">
        <img src={zeeplogo} alt="icon" className="alert-icon" />
        <p>{message}</p>
        <div className="confirm-buttons">
        <button className="confirm-btn" onClick={onConfirm}>확인</button>
        <button className="cancel-btn" onClick={onCancel}>취소</button>          
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;
